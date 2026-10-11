# Stein Food Hub

A Java 17 Swing order manager with separate models, storage, services, and screens. **New order** contains order entry and the complete order list. **Receipts** shows saved receipt history, and **Settings** contains workspace preferences.

## Build and run

Install a JDK 17 or newer and ensure `java` and `javac` are on `PATH`. No third-party libraries or database are required.

From PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

To compile and run the dependency-free behavior checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Test
```

The execution-policy option applies only to that PowerShell process. The scripts use their own directory as the working directory, compile with `--release 17 -encoding UTF-8`, and copy `resources/` into `build/classes/`. In Eclipse or VS Code, import the existing Java project and run `finalPPKG.finalProject_OBAOB3`; `.classpath` includes the resource directory and uses `bin/` for IDE output. Keeping IDE output separate from the scripts prevents resource-copy conflicts.

If VS Code reports an existing resource in an old output folder after changing the project setup, open the Command Palette and run **Java: Clean Java Language Server Workspace**, then allow the Java extension to reload and rebuild the project.

## Windows installer

When packaging with JDK 27, include `--win-with-ui --win-dir-chooser --win-shortcut-prompt` to show a setup wizard with installation-folder and shortcut choices. `--win-menu` and `--win-shortcut` create shortcuts; they do not enable the setup wizard by themselves. See the [jpackage Windows options](https://docs.oracle.com/en/java/javase/27/docs/specs/man/jpackage.html).

After installation, open **SteinFoodHub** from its desktop or Start menu shortcut. The per-user installation is normally at `%LOCALAPPDATA%\SteinFoodHub\SteinFoodHub.exe`. The setup EXE installs the application without launching it afterward.

## Find the code and assets

| Location | Responsibility |
| --- | --- |
| `src/finalPPKG/finalProject_OBAOB3.java` | Application entry point |
| `src/finalPPKG/app/` | Configuration, controller, and application actions |
| `src/finalPPKG/model/` | Immutable order and menu models |
| `src/finalPPKG/repository/` | Order storage interface and file implementation |
| `src/finalPPKG/service/` | Order operations, menu loading, and receipt formatting |
| `src/finalPPKG/ui/views/` | Screens and their shared abstract base |
| `src/finalPPKG/ui/components/` | Reusable buttons, cards, icons, and order tables |
| `src/finalPPKG/ui/theme/` | Shared colors, fonts, and styling |
| `resources/` | Editable application resources |
| `tests/finalPPKG/tests/` | Behavior checks without a test framework |

The visual assets are deliberately small and easy to locate:

| Asset | Edit here |
| --- | --- |
| Menu categories and dish names | [resources/menu.csv](resources/menu.csv) |
| Colors, typography, and spacing | [Theme.java](src/finalPPKG/ui/theme/Theme.java) |
| Navigation and action icons | [AppIcon.java](src/finalPPKG/ui/components/AppIcon.java) |
| Rounded cards and buttons | [RoundedPanel.java](src/finalPPKG/ui/components/RoundedPanel.java), [AppButton.java](src/finalPPKG/ui/components/AppButton.java) |
| Order table columns and values | [OrderTableModel.java](src/finalPPKG/ui/components/OrderTableModel.java) |

Icons are drawn in Java, so there are no hidden image files or external downloads. Edit `menu.csv` to change dish names or categories; its columns are `category,name`. Unit prices are entered when ordering, since the original project did not define a menu price list.

## OOP structure

**Abstraction and inheritance:** [WorkspacePanel](src/finalPPKG/ui/views/WorkspacePanel.java) defines the shared screen layout and refresh contract. Each screen extends it and provides its own content and behavior.

The concrete screens are `NewOrderPanel`, `ReceiptPanel`, and `SettingsPanel`. `OrderListPanel` embeds search, edit, remove, and checkout in New order. `OrderFormPanel` shares input between new orders and editing. `ReceiptDialogPanel` supplies the receipt modal content. `AppShell` arranges navigation and statistics, and `AppFrame` hosts it in a window.

**Encapsulation:** [Order](src/finalPPKG/model/Order.java) stores private, immutable fields and owns validation and money calculations. The UI cannot modify an order's totals independently of its quantity, price, or discount.

**Interfaces and polymorphism:** [OrderRepository](src/finalPPKG/repository/OrderRepository.java) defines `load()` and `save()`. `OrderService` works with that interface; the application uses `FileOrderRepository`, while tests use an in-memory implementation that can simulate failures. Screens request work through [OrderActions](src/finalPPKG/app/OrderActions.java).

**Separation of responsibilities:** Screens collect input and show feedback. `OrderService` changes orders, the repository reads and writes files, and `ReceiptService` formats and exports receipts. A successful save publishes a new immutable snapshot; a failed save leaves the current orders intact.

To add another storage backend, implement `OrderRepository` and supply it to `OrderService`. To add a screen, extend `WorkspacePanel` and connect its actions through `OrderActions`.

Call `OrderService.refresh()` successfully before modifying orders. A failed refresh keeps the last readable snapshot and blocks saving until a subsequent refresh succeeds, so unreadable history cannot be overwritten by stale data.

## Data and validation

Orders are stored in `order_history.txt` in the working directory. `run.ps1` sets that directory to the project root. To store files elsewhere:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
java '-Dstein.data.dir=C:\Users\Admin\SteinOrders' -cp build/classes finalPPKG.finalProject_OBAOB3
```

`AppConfig` resolves the data directory and the `receipt-history/` archive. Export dialogs suggest `receipt-<number>.txt`. The application accepts the original five-column order history format:

```text
Dish : Quantity : Unit price : Discount percent : Total
Pork Adobo : 3 : 19.99 : 12.5 : 52.47
```

The first line above describes the fields; it is not a header to put in the data file. Legacy numeric totals are validated and recalculated from the order fields. Invalid history rows stop loading and leave the file unchanged. Back up history before editing it manually.

Quantity must be 1–9,999, unit price 0–999,999,999.99, and discount 0–100%. Dish names cannot contain colons or line breaks. Decimal values accept up to six decimal places; prices and discount amounts round to two places using `HALF_UP`. Calculations use `BigDecimal`. For example, three items at PHP 19.99 with a 12.5% discount produce a PHP 59.97 subtotal, PHP 7.50 discount, and PHP 52.47 total.

## Checkout and receipt history

1. Add dishes and manage the full order list on **New order**.
2. Click **Checkout** to open a receipt modal for every current order.
3. Use **Save receipt**, **Print**, or **Export**. Saving or exporting adds the receipt to history; exporting updates the same entry rather than duplicating it.
4. On **Receipts**, click a history row or select it and click **Open receipt** to view and print its original snapshot.
5. Check one or more boxes in the **Select** column and click **Delete selected**, or use **Clear cache** to remove all saved receipt history. Both actions ask for confirmation and preserve exported text files and current orders. Clicking a checkbox selects the receipt without opening its modal.

Receipts are saved as individual UTF-8 `.receipt` files in `receipt-history/` inside the configured data directory. Each stores its ID, creation time, original order lines, and export path. The history survives restarting the application, changing prices, removing orders, and clearing the current order list. Checkout keeps current orders until you explicitly clear them. Existing `receipt.txt` files remain available on disk; new saved/exported receipt snapshots populate the history table.

Receipt cleanup removes the app's `.receipt` archive files. After a deletion error, the table reflects completed deletions and cleanup remains locked until **Refresh** succeeds.

The behavior checks cover calculations, validation, immutable snapshots, selected-row edits and removals, failed writes, legacy history, malformed data, menu resources, receipt persistence, receipt export, deletion after restarting, cache clearing, and partial deletion failures. They create and remove temporary test files; they do not use the user's order history.

The Swing smoke test runs on the event-dispatch thread without opening a window. It checks live form validation, storage locking, sorted and filtered row selection, checkout, receipt-row clicks, checkbox selection after sorting, cleanup controls, modal save/export/print callbacks, compact rows, and smaller-window access. It writes previews of the three screens and the receipt modal to `build/preview/`. Printer dialogs and desktop window interactions require a manual check when running the application.
