package Ir.hamed.dnseye;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import Ir.hamed.dnseye.vservice.VhostsService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Small UDP/DNS latency benchmark used before the VPN is configured. */
public final class DnsBenchmark {
    private DnsBenchmark() {}

    public static final class Result {
        public final String server;
        public final long latencyMs;
        Result(String server, long latencyMs) { this.server = server; this.latencyMs = latencyMs; }
    }

    public static List<String> getCandidateServers(Context context, SharedPreferences prefs) {
        List<String> servers = new ArrayList<>();
        boolean onlyBundled = prefs.getBoolean(SettingsFragment.DNS_ONLY_BUNDLED, false);
        for (String ip : DnsListRepository.allServers(context)) {
            if (!servers.contains(ip)) servers.add(ip);
        }
        // Public fallbacks are useful unless the user explicitly limits tests to the DNS list.
        if (!onlyBundled) Collections.addAll(servers,
                "1.1.1.1", "1.0.0.1",
                "8.8.8.8", "8.8.4.4",
                "9.9.9.9", "149.112.112.112",
                "94.140.14.14", "94.140.15.15",
                "208.67.222.222", "208.67.220.220",
                "185.228.168.9", "185.228.169.9",
                "76.76.2.0", "76.76.10.0");
        try {
            JSONArray arr = new JSONArray(prefs.getString(SettingsFragment.CUSTOM_DNS_LIST, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String ip = arr.getJSONObject(i).optString("ip", "").trim();
                if (!ip.isEmpty() && !servers.contains(ip)) servers.add(ip);
            }
        } catch (Exception ignored) {}
        return servers;
    }

    public static List<Result> test(final List<String> servers, final int timeoutMs) {
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(8, servers.size())));
        List<Future<Result>> futures = new ArrayList<>();
        for (final String server : servers) {
            futures.add(pool.submit(new Callable<Result>() {
                @Override public Result call() { return testOne(server, timeoutMs); }
            }));
        }
        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) {
            try {
                Result r = f.get(timeoutMs + 500L, TimeUnit.MILLISECONDS);
                if (r != null) results.add(r);
            } catch (Exception ignored) {}
        }
        pool.shutdownNow();
        Collections.sort(results, new Comparator<Result>() {
            @Override public int compare(Result a, Result b) { return Long.compare(a.latencyMs, b.latencyMs); }
        });
        return results;
    }

    private static Result testOne(String server, int timeoutMs) {
        DatagramSocket socket = null;
        try {
            InetAddress address = InetAddress.getByName(server);
            socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs);
            // If the app VPN is already running, keep the benchmark outside the VPN tunnel.
            VhostsService.protectSocket(socket);
            byte[] query = buildQuery();
            long started = System.nanoTime();
            socket.send(new DatagramPacket(query, query.length, address, 53));
            byte[] response = new byte[512];
            DatagramPacket packet = new DatagramPacket(response, response.length);
            socket.receive(packet);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (packet.getLength() < 12) return null;
            return new Result(server, Math.max(1, elapsed));
        } catch (Exception ignored) {
            return null;
        } finally {
            if (socket != null) socket.close();
        }
    }

    private static byte[] buildQuery() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(64);
        b.put((byte) 0x4D).put((byte) 0x53); // transaction id
        b.putShort((short) 0x0100);         // recursion desired
        b.putShort((short) 1);              // QDCOUNT
        b.putShort((short) 0).putShort((short) 0).putShort((short) 0);
        for (String label : new String[]{"example", "com"}) {
            byte[] bytes = label.getBytes("UTF-8");
            b.put((byte) bytes.length).put(bytes);
        }
        b.put((byte) 0).putShort((short) 1).putShort((short) 1); // A / IN
        byte[] query = new byte[b.position()];
        b.flip(); b.get(query);
        return query;
    }
}
