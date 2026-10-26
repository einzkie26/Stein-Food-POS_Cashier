# Stein Food Hub

A Java 17 Swing order manager with separate models, storage, services, and screens. **New order** contains order entry and the complete order list. **Receipts** shows saved receipt history, and **Settings** contains workspace preferences.

The counter-service design uses a cream background, chili-red navigation and actions, gold accents, and food icons. Order entry gets more space than the live item summary, with consistent 44-pixel controls and 16-pixel gaps between cards. The full current-order total appears beside Checkout. The window opens at up to 1280 × 880, fits the available screen, and keeps the workspace scrollable at smaller sizes.

The visual assets are deliberately small and easy to locate:

| Asset | Edit here |
| --- | --- |
| Menu categories and dish names | [resources/menu.csv](resources/menu.csv) |
| Colors, typography, and spacing | [Theme.java](src/finalPPKG/ui/theme/Theme.java) |
| Navigation and action icons | [AppIcon.java](src/finalPPKG/ui/components/AppIcon.java) |
| Rounded cards and buttons | [RoundedPanel.java](src/finalPPKG/ui/components/RoundedPanel.java), [AppButton.java](src/finalPPKG/ui/components/AppButton.java) |
| Order table columns and values | [OrderTableModel.java](src/finalPPKG/ui/components/OrderTableModel.java) |

Icons are drawn in Java, so there are no hidden image files or external downloads. Edit `menu.csv` to change dish names or categories; its columns are `category,name`. Unit prices are entered when ordering, since the original project did not define a menu price list.

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
