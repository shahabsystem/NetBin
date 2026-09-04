# NETBIN

NETBIN (`Ir.hamed.dnseye`) is a modern Android local-VPN DNS/Hosts tool based on the original VHosts engine.

## DNS management
- Add unlimited custom IPv4 DNS servers.
- Built-in DNS profiles for Cloudflare, Google, Quad9, AdGuard and OpenDNS.
- Manual primary/backup selection.
- Automatic DNS benchmarking at connection time; the fastest two reachable servers are applied.
- One-tap DNS speed test from Settings.
- Default online Hosts source: `https://raw.githubusercontent.com/shahabsystem/VhostchizPn/main/hosts.txt`.

## Features
- Local VPN DNS interception without root
- Hosts and wildcard DNS support
- Built-in ad/tracker blocking
- Optional remote blocklist
- Primary and backup custom DNS
- Persian UI
- Light / dark / system theme
- JSON settings import/export
- Startup support, Quick Settings tile and widget
- Developer support links

## Build
```bash
./gradlew assembleGithubDebug
```

GitHub Actions automatically builds the GitHub Debug APK on pushes and pull requests.

## Package
`Ir.hamed.dnseye`

## Persian 
NETBIN؛ اینترنتی که قرار نیست پول ترافیکش را برای هیچ بدهیم!

سال‌هاست کاربران ایرانی در دنیای اینترنت با محدودیت‌های مختلف دست‌وپنجه نرم می‌کنند؛ از محدودیت‌ها و تحریم‌های اعمال‌شده توسط برخی کشورها و سرویس‌های بین‌المللی گرفته تا فیلترینگ و محدودیت‌های داخلی. نتیجه هم چیزی است که همه‌مان کم‌وبیش تجربه کرده‌ایم: گاهی برای دسترسی به یک سرویس ساده، باید از چند مرحله عبور کنیم که خودشان یک بازی کامپیوتری کامل محسوب می‌شوند! 😄

اما داستان فقط دسترسی نیست.

وقتی برای عبور از محدودیت‌ها مجبور می‌شویم از ابزارها و مسیرهای مختلف استفاده کنیم، بخشی از ترافیک اینترنت ما ممکن است صرف تبلیغات، Trackerها، سرویس‌های غیرضروری و درخواست‌هایی شود که اصلاً نمی‌دانیم چه زمانی و چرا در حال اجرا هستند.

یعنی شما فقط آمده‌اید یک صفحه را باز کنید، اما قبل از اینکه محتوای موردنظرتان نمایش داده شود، چندین سرویس تبلیغاتی تصمیم گرفته‌اند در این مهمانی شرکت کنند!

و البته هزینه این مهمانی را هم شما می‌دهید. 😐😂

اینجا NETBIN وارد می‌شود!

NETBIN با هدف مدیریت بهتر DNS و Hosts و کمک به پاکسازی بخشی از ترافیک ناخواسته شبکه توسعه داده شده است.

برنامه تلاش می‌کند با استفاده از فهرست‌های Hosts و مسدودسازی دامنه‌های تبلیغاتی و Trackerها، جلوی بخشی از ارتباطات غیرضروری را بگیرد؛ ارتباطاتی که ممکن است علاوه بر ایجاد مزاحمت، باعث مصرف ترافیک و منابع دستگاه شوند.

از طرف دیگر، NETBIN امکان مدیریت DNS را در اختیار شما قرار می‌دهد تا بتوانید DNSهای مختلف را اضافه، حذف و آزمایش کنید و در صورت تمایل، سریع‌ترین گزینه را برای اتصال خود انتخاب کنید.

حتی اگر حوصله تست کردن تک‌تک DNSها را ندارید، برنامه می‌تواند این کار را برایتان انجام دهد.

😎 خلاصه اینکه NETBIN چه کار می‌کند؟

🔹 مدیریت و انتخاب DNS
🔹 امکان اضافه کردن DNSهای دلخواه
🔹 تست سرعت DNSها
🔹 انتخاب خودکار DNS سریع‌تر
🔹 پشتیبانی از DNSهای ایرانی و عمومی
🔹 مدیریت فهرست Hosts
🔹 مسدودسازی دامنه‌های تبلیغاتی و Trackerها
🔹 امکان دریافت فهرست‌ها از اینترنت
🔹 امکان بارگذاری تنظیمات از لینک
🔹 پشتیبانی از تم روشن و تاریک
🔹 و کلی گزینه برای کسانی که دوست دارند بدانند اینترنتشان دقیقاً چه غلطی می‌کند! 😂

🧹 فلسفه NETBIN ساده است:

اگر ترافیک اینترنت قرار است مصرف شود، حداقل برای چیزی مصرف شود که خودمان خواسته‌ایم!

قرار نیست وقتی یک صفحه را باز می‌کنیم، ده‌ها دامنه تبلیغاتی و Tracker هم پشت سرمان وارد شوند و بگویند:

«سلام! ما هم اومدیم! فقط چند مگابایت ترافیک می‌خوریم و میریم!» 😂

NETBIN تلاش می‌کند این مهمان‌های ناخوانده را تا حد امکان شناسایی و مسدود کند.

البته NETBIN قرار نیست معجزه کند یا تمام محدودیت‌های اینترنت را یک‌شبه از بین ببرد؛ هدف آن مدیریت بهتر اتصال، DNS و ترافیک ناخواسته و فراهم کردن کنترل بیشتر برای کاربر است.

🌐 NETBIN
اینترنت خودت را بهتر بشناس، DNS خودت را انتخاب کن و اجازه نده هر دامنه‌ای بی‌دعوت وارد مهمانی شود!

ساخته شده برای کاربرانی که از اینترنت استفاده می‌کنند؛ نه برای تبلیغاتی که از کاربر استفاده می‌کنند! 😄

❤️ اگر NETBIN برایتان مفید بود، از توسعه آن حمایت کنید و با معرفی برنامه به دوستانتان کمک کنید پروژه ادامه پیدا کند.




## Support
- GitHub: https://github.com/shahabsystem
- Email: hamedmohammadinikche@gmail.com
- Coffee: https://coffeebede.com/shahabsystem
- Reymit: https://reymit.ir/shahabsystem
