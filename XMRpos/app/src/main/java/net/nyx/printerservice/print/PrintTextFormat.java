package net.nyx.printerservice.print;

// https://github.com/SZNoryox/PrinterClient
import android.os.Parcel;
import android.os.Parcelable;

public class PrintTextFormat implements Parcelable {

    private int textSize = 24;// 字符串大小,px
    private boolean underline = false;// 下划线
    private float textScaleX = 1.0f;// 字体的横向缩放 参数值0-1表示字体缩小 1表示正常 大于1表示放大
    private float textScaleY = 1.0f;
    private float letterSpacing = 0;// 列间距
    private float lineSpacing = 0;//行间距
    private int topPadding = 0;
    private int leftPadding = 0;
    private int ali = 0;// 对齐方式, 默认0. 0--LEFT, 1--CENTER, 2--RIGHT
    private int style = 0; // 字体样式, 默认0. 0--NORMAL, 1--BOLD, 2--ITALIC, 3--BOLD_ITALIC
    private int font = 0; // 字体, 默认0. 0--DEFAULT, 1--DEFAULT_BOLD, 2--SANS_SERIF, 3--SERIF, 4--MONOSPACE, 5--CUSTOM
    private String path; // 自定义字库文件路径

    public PrintTextFormat() {
    }

    /**
     * Return the text size. The default value is 24
     *
     * @return /
     */
    public int getTextSize() {
        return textSize;
    }

    /**
     * Set the pixel size of text
     *
     * @param textSize /
     */
    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    /**
     * Whether the text has underline
     *
     * @return /
     */
    public boolean isUnderline() {
        return underline;
    }

    /**
     * Set the text underline
     *
     * @param underline /
     */
    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    /**
     * Return the horizontal skew factor for text. The default value is 1.0
     *
     * @return /
     */
    public float getTextScaleX() {
        return textScaleX;
    }

    /**
     * Set the horizontal scale factor for print text. The default value
     * is 1.0. Values > 1.0 will stretch the text wider. Values < 1.0 will
     * stretch the text narrower.
     *
     * @param textScaleX /
     */
    public void setTextScaleX(float textScaleX) {
        this.textScaleX = textScaleX;
    }

    /**
     * Return the vertical skew factor for text. The default value is 1.0
     *
     * @return /
     */
    public float getTextScaleY() {
        return textScaleY;
    }

    public void setTextScaleY(float textScaleY) {
        this.textScaleY = textScaleY;
    }

    /**
     * Return the paint's letter-spacing for text. The default value is 0.
     *
     * @return /
     */
    public float getLetterSpacing() {
        return letterSpacing;
    }

    /**
     * Set the letter-spacing for print text. The default value is 0.
     * The value is in 'EM' units.  Typical values for slight expansion
     * will be around 0.05.  Negative values tighten text
     *
     * @param letterSpacing /
     */
    public void setLetterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
    }

    /**
     * Get the line-spacing for print text.
     *
     * @return /
     */
    public float getLineSpacing() {
        return lineSpacing;
    }

    /**
     * Set the line-spacing for print text.
     *
     * @param lineSpacing /
     */
    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public int getTopPadding() {
        return topPadding;
    }

    public void setTopPadding(int topPadding) {
        this.topPadding = topPadding;
    }

    public int getLeftPadding() {
        return leftPadding;
    }

    public void setLeftPadding(int leftPadding) {
        this.leftPadding = leftPadding;
    }

    /**
     * Return text alignment
     *
     * @return /
     */
    public int getAli() {
        return ali;
    }

    /**
     * Set text alignment
     *
     * @param ali 0--LEFT, 1--CENTER, 2--RIGHT
     */
    public void setAli(int ali) {
        this.ali = ali;
    }

    /**
     * Return print text style
     *
     * @return The style (normal, bold, italic) of the typeface.
     */
    public int getStyle() {
        return style;
    }

    /**
     * Set print text style
     *
     * @param style The style (normal, bold, italic) of the typeface.
     *              e.g. NORMAL, BOLD, ITALIC, BOLD_ITALIC
     *              0--NORMAL, 1--BOLD, 2--ITALIC, 3--BOLD_ITALIC;
     */
    public void setStyle(int style) {
        this.style = style;
    }

    /**
     * Return print text style
     *
     * @return /
     */
    public int getFont() {
        return font;
    }

    /**
     * Set print text font from the specified font type.
     *
     * @param font The font that has built-in. Default 0
     *             0--DEFAULT, 1--DEFAULT_BOLD, 2--SANS_SERIF, 3--SERIF, 4--MONOSPACE, 5--CUSTOM
     */
    public void setFont(int font) {
        this.font = font;
    }

    /**
     * The path of custom text font
     *
     * @return /
     */
    public String getPath() {
        return path;
    }

    /**
     * Set custom print text font from the specified font file.
     *
     * @param path The full path to the font file.
     */
    public void setPath(String path) {
        this.path = path;
    }

    protected PrintTextFormat(Parcel in) {
        textSize = in.readInt();
        underline = in.readByte() != 0;
        textScaleX = in.readFloat();
        textScaleY = in.readFloat();
        letterSpacing = in.readFloat();
        lineSpacing = in.readFloat();
        topPadding = in.readInt();
        leftPadding = in.readInt();
        ali = in.readInt();
        style = in.readInt();
        font = in.readInt();
        path = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(textSize);
        dest.writeByte((byte) (underline ? 1 : 0));
        dest.writeFloat(textScaleX);
        dest.writeFloat(textScaleY);
        dest.writeFloat(letterSpacing);
        dest.writeFloat(lineSpacing);
        dest.writeInt(topPadding);
        dest.writeInt(leftPadding);
        dest.writeInt(ali);
        dest.writeInt(style);
        dest.writeInt(font);
        dest.writeString(path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PrintTextFormat> CREATOR = new Creator<PrintTextFormat>() {
        @Override
        public PrintTextFormat createFromParcel(Parcel in) {
            return new PrintTextFormat(in);
        }

        @Override
        public PrintTextFormat[] newArray(int size) {
            return new PrintTextFormat[size];
        }
    };
}