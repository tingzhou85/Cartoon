package txtreaderlib.main;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import txtreaderlib.bean.EnChar;
import txtreaderlib.bean.NumChar;
import txtreaderlib.bean.Page;
import txtreaderlib.bean.TxtChar;
import txtreaderlib.bean.TxtLine;
import txtreaderlib.interfaces.IPage;
import txtreaderlib.interfaces.IPageDataPipeline;
import txtreaderlib.interfaces.IParagraphData;
import txtreaderlib.interfaces.ITxtLine;
import txtreaderlib.utils.FormatUtil;
import txtreaderlib.utils.TextBreakUtil;


public class PageDataPipeline implements IPageDataPipeline {
    private TxtReaderContext readerContext;

    public PageDataPipeline(TxtReaderContext readerContext) {
        this.readerContext = readerContext;
    }

    @Override
    public IPage getPageStartFromProgress(int paragraphIndex, int charIndex) {
        IParagraphData data = readerContext.getParagraphData();
        int PageLineNum = readerContext.getPageParam().PageLineNum;
        int CurrentPaIndex = paragraphIndex;
        int startIndex = charIndex;
        int ParagraphNum = data.getParagraphNum();
        float lineWidth = readerContext.getPageParam().LineWidth;
        float textPadding = readerContext.getPageParam().TextPadding;

        if (CurrentPaIndex >= ParagraphNum || CurrentPaIndex < 0) return null;//超过返回null

        IPage page = new Page();

        while (page.getLineNum() < PageLineNum && CurrentPaIndex < ParagraphNum) {
            String paragraphStr = data.getParagraphStr(CurrentPaIndex);
            if (paragraphStr != null && paragraphStr.length() > 0) {
                List<ITxtLine> lines = getLineFromParagraphStartOnBeginning(paragraphStr, CurrentPaIndex, startIndex, lineWidth, textPadding, readerContext.getPaintContext().textPaint);
                for (ITxtLine line : lines) {
                    page.addLine(line);
                    if (page.getLineNum() >= PageLineNum) {
                        break;
                    }
                }

                startIndex = 0;
            }
            CurrentPaIndex++;
        }

        return page;
    }

    @Override
    public IPage getPageEndToProgress(int paragraphIndex, int charIndex) {

        IParagraphData data = readerContext.getParagraphData();
        int PageLineNum = readerContext.getPageParam().PageLineNum;
        int CurrentPaIndex = paragraphIndex;
        int startIndex = charIndex;
        int ParagraphNum = data.getParagraphNum();
        float lineWidth = readerContext.getPageParam().LineWidth;
        float textPadding = readerContext.getPageParam().TextPadding;
        if (charIndex == 0) {//说明上页开始是段落开始位置，段落左移
            CurrentPaIndex--;//
        }
        if (CurrentPaIndex >= ParagraphNum || CurrentPaIndex < 0) return null;//超过返回null
        IPage page = new Page();
        while (page.getLineNum() < PageLineNum && CurrentPaIndex >= 0) {
            String paragraphStr = data.getParagraphStr(CurrentPaIndex);
            if (paragraphStr != null && paragraphStr.length() > 0) {
                if(charIndex==0){//说明上页开始是段落开始位置
                    startIndex = paragraphStr.length();
                }
                List<ITxtLine> lines = getLineFromParagraphOnEnd(paragraphStr, CurrentPaIndex, startIndex, lineWidth, textPadding, readerContext.getPaintContext().textPaint);
                if (lines.size() > 0) {
                    for (int i = lines.size() - 1; i >= 0; i--) {
                        ITxtLine line = lines.get(i);
                        page.addLine(line);
                        if (page.getLineNum() >= PageLineNum) {
                            break;
                        }
                    }
                }
            }
            CurrentPaIndex--;
        }

        if (page.HasData()) {
            Collections.reverse(page.getLines());
        }
        return page;
    }

    /**
     * @param paragraphData  完整段落数据
     * @param paragraphIndex 段落下标
     * @param startCharIndex 开始字符位置
     * @param lineWidth      行宽度
     * @param textPadding    字体间距
     * @param paint          画笔
     * @return startCharIndex超过范围，返回空集合
     */
    //开始满行
    private List<ITxtLine> getLineFromParagraphStartOnBeginning(String paragraphData, int paragraphIndex, int startCharIndex,
                                                                float lineWidth, float textPadding, Paint paint) {
        List<ITxtLine> lines = new ArrayList<>();
        int startIndex = startCharIndex;
        startIndex = startIndex < 0 ? 0 : startIndex;

        if (paragraphData == null || startIndex >= paragraphData.length()) {
            return lines;
        }

        if (paragraphData != null && paragraphData.length() > 0) {
            String s = paragraphData.substring(startIndex);//截取要的数据

            while (s.length() > 0) {// is[0] 为个数 is[1] 为是否满一行
                float[] is = TextBreakUtil.BreakText(s, lineWidth, textPadding, paint);
                ITxtLine line;
                if (is[1] != 1) {//不满一行
                    line = getLineFromString(s, paragraphIndex, startIndex, is);
                    lines.add(line);
                    break;
                }
                int num = (int) is[0];
                String lineStr = s.substring(0, num);
                line = getLineFromString(lineStr, paragraphIndex, startIndex, is);
                startIndex = startIndex + num;
                if (line != null) {
                    lines.add(line);
                }
                s = s.substring(num);
            }
        }
        return lines;
    }

    /**
     * @param paragraphData
     * @param paragraphIndex
     * @param endCharIndex
     * @param lineWidth
     * @param textPadding
     * @param paint
     * @return startCharIndex等于0，返回空集合，超过范围，为段落长度数据
     */
    //结束满行
    private List<ITxtLine> getLineFromParagraphOnEnd(String paragraphData, int paragraphIndex, int endCharIndex,
                                                     float lineWidth, float textPadding, Paint paint) {
        List<ITxtLine> lines = new ArrayList<>();
        int startIndex = 0;
        if (paragraphData == null || endCharIndex <= 0) {
            return lines;
        }

        endCharIndex = endCharIndex >= paragraphData.length() ? paragraphData.length() : endCharIndex;

        if (endCharIndex > 0) {
            String s = paragraphData.substring(startIndex, endCharIndex);//截取要的数据
            while (s.length() > 0) {// is[0] 为个数 is[1] 为是否满一行
                float[] is = TextBreakUtil.BreakText(s, lineWidth, textPadding, paint);
                ITxtLine line;
                int num = (int) is[0];
                if (is[1] != 1) {//不满一行
                    line = getLineFromString(s, paragraphIndex, startIndex, is);
                    lines.add(line);
                    return lines;
                }

                String lineStr = s.substring(0, num);
                line = getLineFromString(lineStr, paragraphIndex, startIndex, is);
                startIndex = startIndex + num;
                if (line != null) {
                    lines.add(line);
                }
                s = s.substring(lineStr.length());
            }
        }
        return lines;
    }


    private ITxtLine getLineFromString(String str, int ParagraphIndex, int charIndex, float[] is) {
        ITxtLine line = null;
        int index = charIndex;
        int cIndex = 2;//找到字符宽度
        if (str != null && str.length() > 0) {
            line = new TxtLine();

            char[] cs = str.toCharArray();
            for (char c : cs) {
                TxtChar Char;
                if (FormatUtil.isDigital(c)) {
                    Char = new NumChar(c);
                } else if (FormatUtil.isLetter(c)) {
                    Char = new EnChar(c);
                } else {
                    Char = new TxtChar(c);
                }
                Char.ParagraphIndex = ParagraphIndex;
                Char.CharIndex = index++;
                Char.CharWidth = is[cIndex++];
                line.addChar(Char);
            }

        }
        return line;
    }
}
