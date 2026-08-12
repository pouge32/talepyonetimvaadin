# My Application

A Spring Boot + Vaadin project. Build your UI in pure Java — no HTML, no JavaScript.

> **New to Vaadin?** The 5-minute [Quickstart](https://vaadin.com/quickstart) walks you from here to your first running app, a live code change, and an AI-assisted edit with Copilot.

---

## Fastest start — no plugin needed

From the project folder:

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

No system Maven required — the wrapper is included. Then open **http://localhost:8080**.

The first start takes ~30 seconds while Maven downloads dependencies. You'll get a runnable **Task List** app: a data grid (Description / Due Date / Creation Date), a Create button, and an empty-state message. When you see that, you're running.

> **Port 8080 already in use?** Stop the other process, or set `server.port=8081` in `src/main/resources/application.properties` and open that port instead.
>
> **To stop the app:** press `Ctrl+C` in the terminal (or the red Stop button if you launched from your IDE).

## Optional upgrade — instant hotswap

Running with `spring-boot:run` works, but Java code changes need a restart. For **live reload** — edit Java, see it in the browser without restarting — install the **Vaadin plugin** and start the app through it:

- **IntelliJ IDEA:** install *Vaadin* from the JetBrains Marketplace → **Debug using Hotswap Agent** (dropdown next to Run). *Just installed it? Let IntelliJ finish indexing, or restart it, if the menu item isn't there yet.*
- **VS Code:** install the *Vaadin* extension → **Vaadin: Debug using Hotswap Agent** from the command palette.
- **Eclipse:** install the *Vaadin* plugin → right-click the project → **Run As → Vaadin Application**.

This is what makes the edit-and-see-it loop feel instant — and it's required for the AI edits in [Vaadin Copilot](https://vaadin.com/docs/latest/tools/copilot).

---

## Day 2: make your task list interactive (~10 min)

Your app lists tasks. Let's make a row do something when you click it.

**1. Add a click listener (by hand).** In `src/main/java/com/example/examplefeature/ui/TaskListView.java`, add this after the `taskGrid.addColumn(...)` block:

```java
taskGrid.addItemClickListener(event ->
    Notification.show("Due: " + Optional.ofNullable(event.getItem().getDueDate())
        .map(LocalDate::toString)
        .orElse("no due date")));
```

Add the import: `import com.vaadin.flow.component.notification.Notification;`

Save and click a task — a notification shows its due date. That's a server-side event handler, in pure Java.

**2. Let Copilot finish it.** Open Copilot (bottom-right toolbar → **Edit mode**), click the AI assistant, and try:

> When a task row is clicked, open a dialog showing its description, due date, and creation date, with a Close button.

Copilot writes the dialog into `TaskListView.java` for you. Open the file — your new code is right there.

---

## Ask your AI assistant about Vaadin (optional)

If you use Claude Code, Cursor, or another AI coding assistant, connect it to the **Vaadin MCP server** so it answers against real Vaadin docs and the exact API of your installed version — instead of guessing from outdated training data.

```bash
# One-time setup — see https://vaadin.com/docs/latest/building-apps/mcp
```

A `.mcp.json` is included (commented out by default). Uncomment it, or run the setup command above, to activate.

---

## Build for production

```bash
./mvnw package
java -jar target/*.jar
```

## Learn more

- [Vaadin Quickstart](https://vaadin.com/quickstart) — the 5-minute getting-started path
- [Components](https://vaadin.com/docs/latest/components) — 50+ UI components, all callable from Java
- [Vaadin Copilot](https://vaadin.com/docs/latest/tools/copilot) — visual + AI editing in the browser
- [Full documentation](https://vaadin.com/docs)

# Talep Yönetim Sistemi - Önceliklendirme Modülü

## Önceliklendirme Formülü

Sistemimizde taleplerin (request) öncelik sırasını matematiksel olarak belirlemek için aşağıdaki algoritma kullanılmaktadır:

> **Öncelik Puanı = ((İş Etkisi × 2) × Aciliyet) / Efor**

---

## Algoritmanın Gerekçesi ve Tasarım Kararları

Bu formül, yazılım mühendisliğinde ve Çevik (Agile) proje yönetiminde kullanılan WSJF (Weighted Shortest Job First) ve Value/Effort (Değer/Efor) matrisi gibi endüstri standartlarına çok benzeyen, mantığı çok sağlam bir kurgudur. 

Mentörlere veya paydaşlara sunarken formülün matematiği şu profesyonel argümanlarla savunulmaktadır:

### 1. Neden İş Etkisini 2 ile Çarpıyoruz? (Ağırlıklandırma / Weighting)
Bir talebin sadece "acil" olarak işaretlenmesi, şirkete büyük değer katacağı veya büyük bir zararı önleyeceği anlamına gelmez. Örneğin; bir kullanıcı sadece arayüzdeki bir renk kaymasını "Çok Acil" olarak işaretleyebilir. İş etkisini 2 ile çarparak (ağırlığını artırarak), şirketin ana işleyişini bozan veya büyük değer katan işlerin, sadece yüksek sesle istenen (acil) ama önemsiz işlerin önüne geçmesini garanti altına alıyoruz. Temel önceliğimiz her zaman iş değeridir.

### 2. Neden Aciliyet ile Çarpıyoruz? (Zaman Hassasiyeti)
İş etkisi ve aciliyet birbirini katlayan (çarpan) değişkenlerdir. Hem etkisi çok yüksek hem de zamanı çok daralan bir iş, sistemde üstel bir fırlama yapmalıdır. Bu çarpım işlemi, zamanı dolmak üzere olan (SLA ihlaline yaklaşan) kritik hataları listenin en tepesine otomatik olarak iter.

### 3. Neden Efora Bölüyoruz? (Yatırım Getirisi - ROI & Hızlı Kazanımlar)
Formülün mühendislik açısından en zekice kısmı burasıdır. Paydaya eforu koymak, takıma "Hızlı Kazanımlar" (Quick Wins) sağlar. 
Düşün ki etki ve aciliyet puanı tamamen aynı olan A ve B adında iki talep var:

* **A talebi:** 1 saat sürecek *(Düşük efor -> Payda küçük -> Öncelik Puanı **YÜKSEK**)*
* **B talebi:** 5 gün sürecek *(Yüksek efor -> Payda büyük -> Öncelik Puanı **DÜŞÜK**)*

Efora bölmek, az bir çalışma ile (1 saat) sisteme hemen kazandırılabilecek yüksek değerli işlerin aradan hızla çıkarılmasını sağlar.