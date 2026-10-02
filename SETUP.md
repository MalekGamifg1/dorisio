# دليل إعداد وتشغيل تطبيق دوريسيو (Dorisio Setup Guide)

مرحباً بك في **دوريسيو (Dorisio)** — "دوريكم... بشكل حقيقي".
هذا الدليل يوضح خطوات ربط تطبيق Android بمشروع Firebase الحقيقي، وتفعيل المصادقة وقواعد البيانات والأمان والمشرف الأول.

---

## 1. إنشاء مشروع Firebase (Firebase Project)

1. توجه إلى [Firebase Console](https://console.firebase.google.com).
2. انقر على **Add project** (إضافة مشروع) وسمّه `Dorisio` أو اسماً من اختيارك.
3. يمكنك تفعيل Google Analytics أو تخطيه، ثم اضغط **Create Project**.

---

## 2. تسجيل تطبيق Android في Firebase

1. من لوحة تحكم المشروع في Firebase، اضغط على أيقونة **Android** لإضافة تطبيق جديد.
2. أدخل معرف الحزمة (Package name / Application ID) بالضبط:
   ```
   com.aistudio.dorisio.flgbxz
   ```
3. اسم التطبيق: `دوريسيو (Dorisio)`.
4. (اختياري ولكن يُفضل لتسجيل الدخول بـ Google): أضف بصمة شهادة التصحيح `SHA-1`.
   يمكنك الحصول عليها بتشغيل:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. اضغط **Register app**.
6. حمّل ملف **`google-services.json`** وضعه مباشرة داخل مجلد:
   ```
   /app/google-services.json
   ```

---

## 3. تفعيل Firebase Authentication

1. في Firebase Console، انتقل إلى **Build > Authentication**.
2. اضغط **Get Started**.
3. في تبويب **Sign-in method**، قم بتفعيل الموفرين المطلوبين:
   - **Email/Password**: اضغط تفعيل ثم حفظ.
   - **Google**: اضغط تفعيل، واختر بريد الدعم، ثم حفظ.
   - **Facebook**: قم بإنشاء App في Facebook Developer Portal وأدخل App ID و App Secret في Firebase.
   - **GitHub**: قم بإنشاء OAuth App في GitHub Developer Settings وأدخل Client ID و Client Secret في Firebase.

---

## 4. إنشاء Cloud Firestore

1. في القائمة الجانبية، انتقل إلى **Build > Firestore Database**.
2. اضغط **Create database**.
3. اختر الموقع الجغرافي الأقرب (مثلاً `europe-west` أو `us-central`).
4. اختر وضع البداية: **Production mode**.
5. بعد الإنشاء، انتقل إلى تبويب **Rules** وانسخ محتوى ملف `firestore.rules` المرفق في المشروع ثم اضغط **Publish**.

---

## 5. إنشاء Firebase Storage

1. انتقل إلى **Build > Storage**.
2. اضغط **Get Started** في وضع Production.
3. في تبويب **Rules**، الصق محتوى ملف `storage.rules` المرفق واضغط **Publish**.

---

## 6. تفعيل المشرف الأول (First Admin Setup)

تم تأمين صلاحيات المشرف حتى لا يتمكن أي مستخدم عادي من منح نفسه صلاحيات الإدارة:
- **الطريقة الفورية داخل التطبيق**:
  1. ادخل إلى شاشة **"حسابي"**.
  2. اضغط على خيار **"تفعيل صلاحيات المشرف"**.
  3. أدخل الرمز السري المخصص:
     ```
     DORISIO2026
     ```
  4. فور التأكيد، ستتحول رتبة حسابك إلى `⭐ مشرف معتمد (Admin)` وتظهر لك لوحة التحكم الكاملة وزر إدارة البث المباشر.
- **الطريقة عبر Firebase Console (Custom Claims أو Firestore)**:
  في مجموعة `users` في Firestore، افتح مستند المستخدم الخاص بك وعدل الحقل:
  `role: "ADMIN"`.

---

## 7. التحقق والتشغيل

التطبيق مصمم مع نمط **Graceful Offline & Hybrid Sync**:
- إذا لم تكن قد أضفت `google-services.json` بعد، سيعمل التطبيق بكامل وظائفه محلياً ويسمح لك باختبار جدول الترتيب، مركز البث المباشر للأدمن، تسجيل الأهداف والبطاقات، تقييم اللاعبين، وتوزيع الجوائز.
- بمجرد وضع `google-services.json`، يتعرف التطبيق تلقائياً على اتصال Firebase السحابي ويتزامن في الوقت الفعلي مع جميع أجهزة المتابعين والطلاب!
