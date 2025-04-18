package cardindex.dojocardindex.Utils;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@Slf4j
public class BackgroundPageEvent extends PdfPageEventHelper {

    private final Image background;
    private final float opacity;

    public BackgroundPageEvent(Image background, float opacity) {
        this.background = background;
        this.opacity = opacity;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document){

        if (background!= null) {
            PdfContentByte canvas = writer.getDirectContentUnder();

            PdfGState gState = new PdfGState();
            gState.setFillOpacity(opacity); //прозрачност
            canvas.saveState();
            canvas.setGState(gState);

            background.setAbsolutePosition(0,0);
            background.scaleAbsolute(document.getPageSize().getWidth(), document.getPageSize().getHeight());

            try {
                canvas.addImage(background);
            } catch (DocumentException e) {

                log.error("Грешка при добавянето на фон към PDF документ!",e);
                throw new RuntimeException("Грешка при добавянето на фон към PDF документ",e);
            }

            canvas.restoreState();

            //кант на страницата
            Rectangle rectangle = new Rectangle(
                    document.getPageSize().getLeft()+15,
                    document.getPageSize().getBottom()+15,
                    document.getPageSize().getRight()-15,
                    document.getPageSize().getTop()-15

            );
            rectangle.setBorder(Rectangle.BOX);
            rectangle.setBorderWidth(5f);
            rectangle.setBorderColor(new Color(186,210,232));
            canvas.rectangle(rectangle);
        }
    }
}
