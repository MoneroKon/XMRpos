package net.nyx.printerservice.print;

// https://github.com/SZNoryox/PrinterClient
import android.graphics.Bitmap;
import net.nyx.printerservice.print.PrintTextFormat;

interface IPrinterService {

    String getServiceVersion();

    int getPrinterVersion(out String[] ver);

    int getPrinterModel(out String[] model);

    int getPrinterStatus();

    int paperOut(int px);

    int paperBack(int px);

    int printText(String text, in PrintTextFormat textFormat);

    /**
     * Print text
     *
     * @param text       text content
     * @param textFormat text format
     * @param textWidth  maximum text width
     * @param align      The alignment of the maximum text width relative to the printing paper
     *                   The default is 0. 0--Align left, 1--Align center, 2--Align right
     * @return Print result
     */
    int printText2(String text, in PrintTextFormat textFormat, int textWidth, int align);

    /**
     * Print barcode
     *
     * @param content      barcode content
     * @param width        barcode width, px
     * @param height       barcode height, px
     * @param textPosition barcode content text position, the default is 0.
     *                     0--don't print barcode content
     *                     1--content above the barcode
     *                     2--content below the barcode
     *                     3--text print both the top and bottom of the barcode
     * @param align        alignment, the default is 0. 0--Align left, 1--Align center, 2--Align right
     * @return Print result
     */
    int printBarcode(String content, int width, int height, int textPosition, int align);

    /**
     * Print QR code
     *
     * @param content QRCode content
     * @param width   QRCode width, px
     * @param height  QRCode height, px
     * @param align   alignment, the default is 0. 0--Align left, 1--Align center, 2--Align right
     * @return Print result
     */
    int printQrCode(String content, int width, int height, int align);

    /**
     * Print bitmap
     *
     * @param bitmap Android bitmap object
     * @param type   printer render type, the default is 0.
     *               0--black and white bitmap
     *               1--grayscale bitmap, suitable for rich color images
     * @param align  alignment, the default is 0. 0--Align left, 1--Align center, 2--Align right
     * @return Print result
     */
    int printBitmap(in Bitmap bitmap, int type, int align);

    /**
     * Locate the next label
     *
     * @param labelHeight label paper height, px
     * @param labelGap    label paper gap, px
     * @return Result
     */
    int labelLocate(int labelHeight, int labelGap);

    /**
     * Label printing ends, paper automatically moves to the tear-off position.
     * This method must appear in pairs with {@link #labelLocate}
     *
     * @return Result
     */
    int labelPrintEnd();

    /**
     * Auto locate label. This method only can be called on the system that has performed
     * label learning by {@link #labelDetectAuto}
     *
     * @return Result
     */
    int labelLocateAuto();

    /**
     * Label learning by automatic label detection. This method will start label learning
     *
     * @return Result
     */
    int labelDetectAuto();

    /**
     * Label learning has been performed
     *
     * @return Result
     */
    boolean hasLabelLearning();

    /**
     * Clear label learning data
     *
     * @return Result
     */
    int clearLabelLearning();

    /**
     * Print raster bitmap data
     *
     * @param bytes raster bitmap data
     * @return Result
     */
    int printRasterData(in byte[] bytes);

    /**
     * Print ESC/POS commands
     *
     * @param cmd ESC/POS commands
     * @return Result
     */
    int printEscposData(in byte[] cmd);

    /**
     * Print a row of a table
     *
     * @param texts   table row contents
     * @param weights column width weight
     * @param formats text format for each column
     * @return Result
     */
    int printTableText(in String[] texts, in int[] weights, in PrintTextFormat[] formats);

    /**
     * After printing, feed paper to cutting position
     *
     * @return Result
     */
    int printEndAutoOut();

    /**
     * Show bitmap in customer display LCD
     *
     * @param bitmap Android bitmap object
     * @return Result
     * @since PrinterService v1.5.9 only support black-white bitmap
     * @since PrinterService v1.7.6 support colored bitmap
     */
    int showLcdBitmap(in Bitmap bitmap);

    /**
     * Control the status of the customer display LCD
     *
     * @param flag 0--init 1--wakeup LCD 2--sleep LCD 3--clear LCD 4--reset LCD display
     * @return Result
     * @since PrinterService v1.6.3
     */
    int configLcd(int flag);

    /**
     * Open cash drawer
     *
     * @return Result
     * @since PrinterService v1.6.1
     */
    int openCashBox();

    /**
     * Trigger infrared scan
     *
     * @return Result
     * @since PrinterService v1.7.0
     */
    int triggerQscScan();

    /**
     * Set default logo for customer display LCD
     * It is recommended not to change the logo frequently. The number of modifications is less than 5000 times
     *
     * @param bitmap Android bitmap object
     * @return Result
     * @since PrinterService v1.7.9
     */
    int setLcdLogo(in Bitmap bitmap);
}