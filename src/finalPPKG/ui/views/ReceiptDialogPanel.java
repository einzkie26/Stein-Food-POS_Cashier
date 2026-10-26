package finalPPKG.ui.views;

import finalPPKG.app.OrderActions;
import finalPPKG.model.Receipt;
import finalPPKG.service.ReceiptService;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** The same receipt content and actions are used for checkout and history modals. */
public final class ReceiptDialogPanel extends JPanel {
    public ReceiptDialogPanel(Receipt receipt, OrderActions actions, boolean saved, boolean historyReady, Runnable close) {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        UiSupport.padding(this, 20);
        JPanel heading = UiSupport.transparent(new BorderLayout(0, 6));
        JLabel brand = UiSupport.label("STEIN FOOD HUB  /  YOUR RECEIPT", 11, Font.BOLD, Theme.PRIMARY);
        brand.setIcon(new AppIcon(AppIcon.Kind.BURGER, 20, Theme.PRIMARY));
        brand.setIconTextGap(8);
        heading.add(brand, BorderLayout.NORTH);
        heading.add(UiSupport.label("Receipt " + receipt.getNumber(), 24, Font.BOLD, Theme.TEXT), BorderLayout.CENTER);
        add(heading, BorderLayout.NORTH);
        RoundedPanel paper = new RoundedPanel(new BorderLayout());
        UiSupport.padding(paper, 16);
        JTextArea text = new JTextArea(new ReceiptService().format(receipt));
        text.setName("receiptPreview");
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setForeground(Theme.TEXT);
        text.setBackground(Theme.SURFACE);
        text.setCaretPosition(0);
        text.getAccessibleContext().setAccessibleName("Receipt preview");
        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        paper.add(scroll, BorderLayout.CENTER);
        add(paper, BorderLayout.CENTER);
        JPanel footer = UiSupport.transparent(new BorderLayout(12, 0));
        JLabel status = UiSupport.label(saved ? receipt.getStatus() : "Not saved", 12, Font.PLAIN, Theme.MUTED);
        JPanel buttons = UiSupport.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        AppButton save = new AppButton(saved ? "Saved" : "Save receipt", AppButton.Style.SECONDARY);
        save.setName("saveReceipt");
        save.setEnabled(historyReady && !saved);
        AppButton export = new AppButton("Export", AppButton.Style.PRIMARY);
        export.setName("exportReceipt");
        export.setEnabled(historyReady);
        AppButton print = new AppButton("Print", AppButton.Style.SECONDARY);
        print.setName("printReceipt");
        AppButton dismiss = new AppButton("Close", AppButton.Style.SECONDARY);
        save.addActionListener(event -> {
            if (actions.saveReceipt(receipt)) {
                save.setText("Saved");
                save.setEnabled(false);
                status.setText("Saved");
            }
        });
        export.addActionListener(event -> {
            if (actions.exportReceipt(receipt)) {
                save.setText("Saved");
                save.setEnabled(false);
                status.setText("Exported");
            }
        });
        print.addActionListener(event -> actions.printReceipt(receipt));
        dismiss.addActionListener(event -> close.run());
        buttons.add(save);
        buttons.add(print);
        buttons.add(export);
        buttons.add(dismiss);
        footer.add(status, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }
}

