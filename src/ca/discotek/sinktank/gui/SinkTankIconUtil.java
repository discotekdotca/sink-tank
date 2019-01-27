package ca.discotek.sinktank.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class SinkTankIconUtil {
	
	public static final Image ICONS[] = new Image[] {
	    createCrossHairImage(16),
	    createCrossHairImage(32),
	    createCrossHairImage(42)
	};
	
	public static final List<Image> DEEPDIVE_ICON_LIST = Arrays.asList(ICONS);
	
    public static BufferedImage createCrossHairImage(int diameter) {
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        
        int lineWidth = diameter / 20;
        
        BufferedImage image = new BufferedImage(diameter + lineWidth, diameter + lineWidth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHints(rh);
        
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        
        g.setStroke(new BasicStroke(lineWidth));
        

        
        float factor = 2f/3f;
        
        g.setColor(Color.BLACK);
        g.drawOval( 
                (image.getWidth() - (int) (image.getWidth() * factor)) / 2, 
                (image.getHeight() - (int) (image.getHeight() * factor)) / 2, 
                (int) (image.getWidth() * factor), 
                (int) (image.getHeight() * factor));
        
        g.drawLine(image.getWidth() / 2, 0, image.getWidth() / 2, image.getHeight());
        g.drawLine(0, image.getHeight() / 2, image.getWidth(), image.getHeight() / 2);
        
        return image;
    }
    
    public static BufferedImage flipHorizontally(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -image.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(image, null);
    }
    
    public static BufferedImage flipVertically(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-image.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        
        return  op.filter(image, null);
    }
    
    public static BufferedImage flipHorizontallyAndVertically(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
        tx.translate(-image.getWidth(null), -image.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return  op.filter(image, null);
    }

}
