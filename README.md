# Stein Food Hub

A Java 17 Swing order manager with separate models, storage, services, and screens. **New order** contains order entry and the complete order list. **Receipts** shows saved receipt history, and **Settings** contains workspace preferences.

The counter-service design uses a pale blue background, dark-blue navigation, cyan actions and accents, and food icons. Order details shows selectable dish cards with short hover and selection animations. Quantity, price, discount, and the live item total sit beneath the menu, with consistent 44-pixel controls and 16-pixel gaps between sections. The full current-order total appears beside Checkout. The window opens at up to 1280 × 880, fits the available screen, and keeps the workspace scrollable at smaller sizes.

The visual assets are deliberately small and easy to locate:

| Asset | Edit here |
| --- | --- |
| Menu categories and dish names | [resources/menu.csv](resources/menu.csv) |
| Colors, typography, and spacing | [Theme.java](src/finalPPKG/ui/theme/Theme.java) |
| Navigation and action icons | [AppIcon.java](src/finalPPKG/ui/components/AppIcon.java) |
| Original dish and order ICO assets | [src/finalPPKG/ui/components/icons](src/finalPPKG/ui/components/icons) |
| Packaged PNG copies and dish mapping | [resources/icons](resources/icons), [MenuIcons.java](src/finalPPKG/ui/components/MenuIcons.java) |
| Menu cards, selection, and animations | [MenuCard.java](src/finalPPKG/ui/components/MenuCard.java), [MenuCardPicker.java](src/finalPPKG/ui/components/MenuCardPicker.java) |
| Selected-card border frames and checkmark | [cake_border](src/finalPPKG/ui/components/icons/order_details/cake_border), [mark_checked.ico](src/finalPPKG/ui/components/icons/mark_checked.ico), [SelectionBorder.java](src/finalPPKG/ui/components/SelectionBorder.java) |
| Main logo and animated sidebar wordmark | [main-logo.ico](src/finalPPKG/ui/components/icons/main-logo.ico), [sidebar_muffin_text](src/finalPPKG/ui/components/icons/sidebar_muffin_text), [SidebarLogo.java](src/finalPPKG/ui/components/SidebarLogo.java) |
| Rounded cards and buttons | [RoundedPanel.java](src/finalPPKG/ui/components/RoundedPanel.java), [AppButton.java](src/finalPPKG/ui/components/AppButton.java) |
| Order table columns and values | [OrderTableModel.java](src/finalPPKG/ui/components/OrderTableModel.java) |

The menu uses the nine supplied dish icons, and the order overview uses `current_order.ico` and `order_total.ico`. Java-compatible PNG copies live in `resources/icons` and are copied to the classpath by the normal build. After editing the original ICO artwork on Windows, regenerate those copies with `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Convert-UiIcons.ps1`. The remaining interface icons are drawn in Java; no downloads or extra libraries are required.

The supplied main logo appears in the sidebar, window icon, and receipt header. The sidebar wordmark plays `stein_text1.png` through `stein_text5.png` in order, pausing on the resting scenes before looping. All frames share the same display area so the navigation stays still. Playback stops when the logo is hidden or removed; `-Dstein.ui.reduceMotion=true` displays the first frame without animation. The icon conversion script also copies these PNG frames unchanged into the runtime resources.

Click a dish card, or use arrow keys while a card has focus, then enter its price and quantity. Changing dishes clears the previous unit price; clicking the same card preserves it. Cards wrap into fewer columns in smaller windows. Animations stop when cards are hidden or removed; use the JVM option `-Dstein.ui.reduceMotion=true` to switch immediately between states. Edit `menu.csv` to change dish names or categories; its columns are `category,name`. New dishes use a default food icon until mapped in `MenuIcons.java`. Unit prices remain an order-time choice.

The selected dish loops through `selection_border1.png` to `selection_border4.png`, with `mark_checked.ico` replacing the drawn checkmark. The border artwork fits around each card without covering its center. Playback stops on deselection, hiding, removal, or disabling; reduced-motion mode keeps the first border frame. The supplied PNG artwork remains unchanged in the runtime resources.

## Checkout and receipt history

1. Add dishes and manage the full order list on **New order**.
2. Click **Checkout** to open a receipt modal for every current order.
3. Use **Save receipt**, **Print**, or **Export**. Saving or exporting adds the receipt to history; exporting updates the same entry rather than duplicating it.
4. On **Receipts**, click a history row or select it and click **Open receipt** to view and print its original snapshot.
5. Check one or more boxes in the **Select** column and click **Delete selected**, or use **Clear cache** to remove all saved receipt history. Both actions ask for confirmation and preserve exported text files and current orders. Clicking a checkbox selects the receipt without opening its modal.

Receipts are saved as individual UTF-8 `.receipt` files in `receipt-history/` inside the configured data directory. Each stores its ID, creation time, original order lines, and export path. The history survives restarting the application, changing prices, removing orders, and clearing the current order list. Checkout keeps current orders until you explicitly clear them. Existing `receipt.txt` files remain available on disk; new saved/exported receipt snapshots populate the history table.

Receipt cleanup removes the app's `.receipt` archive files. After a deletion error, the table reflects completed deletions and cleanup remains locked until **Refresh** succeeds.

The behavior checks cover calculations, validation, immutable snapshots, selected-row edits and removals, failed writes, legacy history, malformed data, menu resources, receipt persistence, receipt export, deletion after restarting, cache clearing, and partial deletion failures. They create and remove temporary test files; they do not use the user's order history.

The Swing smoke test runs on the event-dispatch thread without opening a window. It checks dish icons from the classpath, card and keyboard selection, price resets, live form validation, storage locking, sorted and filtered row selection, checkout, receipt-row clicks, checkbox selection after sorting, cleanup controls, modal save/export/print callbacks, compact rows, and smaller-window access. It writes previews of the three screens and the receipt modal to `build/preview/`. Printer dialogs and desktop window interactions require a manual check when running the application.
