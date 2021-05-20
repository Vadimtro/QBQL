package qbql.parser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Scrollable;
import javax.swing.text.html.HTMLEditorKit;

import qbql.util.Util;

/**
 * Visualization of Matrix used by Earley and CYK methods
 * see: SQLEarley.main() for example usage
 * see also:  "CYK Performance Optimization" and "Earley Parsing" sections 
 *            in Parsers.doc for detailed explanation; fig 3a and up are snapshots
 *            of the Visual panel
 * @author Vadim Tropashko
 */
public class Visual implements ActionListener {
    //public int[][] skipped = null; // by CYK
    public long[][] visited = null; // by Earley
    List<LexerToken> src;
    Parser par;
    private int zoom = 1;   // ratio between pixel coordinate and matrix pos
    private int offset = 0;
    
    public Map<Long,Integer> causes;   // pair(skipRanges.key,value) -> symbol
    
    int X;   // pixel coordinates; convert to matrix pos dividing by zoom  
    int Y;
    BufferedImage img;
    Matrix matrix;
    
    JLabel matrixImage = null;
    
    private int pixels = 1900; // for UHD resolution
    
    public Visual( final List<LexerToken> s, Parser c ) {
        causes = new HashMap<Long,Integer>();
        src = s;
        visited = new long[src.size()+1][src.size()+1];
        par = c;
        if( src.size() < 30 )
            pixels = 1000;
        //if( c.rules.length < 50 ) 
            //pixels = 400;
        zoom = 1+pixels/src.size(); 
        offset = zoom/2;
        if( zoom == 1 )
            offset = 0;
        final int size = src.size();
        X = (size+1)*zoom; 
        Y = (size+1)*zoom;
        
        //c.visual = this;
    }
    
    public void draw() {
    	matrix.getVisual().draw(matrix);
    }
    public void draw( Matrix m ) {		
            
        img = drawMatrix(m);

        final JFrame frame = new JFrame(par.getClass().getSimpleName()+" Matrix"); //$NON-NLS-1$
        try {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } catch( Exception e ) { //e.g. org.netbeans.ExitSecurityException        	
        }
        final JEditorPane t = new JEditorPane();
        t.setEditorKit(new HTMLEditorKit());
        JScrollPane editorScrollPane = new JScrollPane(t);
        editorScrollPane.setVerticalScrollBarPolicy(
                                                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(1000, pixels+150));
        editorScrollPane.setMinimumSize(new Dimension(100, 100));

        class ScrollablePicture extends JLabel // Canvas
        implements Scrollable, MouseMotionListener, MouseWheelListener {

            public ScrollablePicture() {
                super(new Icon() {
                    public int getIconHeight() {
                        return Y;
                    }
                    public int getIconWidth() {
                        return X;
                    }
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.drawImage(img, 0, 0, Color.orange, null);
                        drawSrcText(g);
                    }

                });
                setAutoscrolls(true); //enable synthetic drag events
                addMouseMotionListener(this); //handle mouse drags
                addMouseWheelListener(this);  //switch symbols
            }
            public void mouseDragged( MouseEvent e ) {
                Rectangle r = new Rectangle(e.getX(), e.getY(), 10, 10);
                scrollRectToVisible(r);
            }
            /**
             * If at different cell update rule panel content
             */
            public void mouseMoved( MouseEvent e ) {
                e.consume();
                int x = e.getX()/zoom;
                int y = e.getY()/zoom;
                if( x > src.size() )
                    return;
                if( y > src.size() )
                    return;
                if( x != x0 || y != y0 ) {
                	index = 0;
                	ambig = 0;
                }
                output = matrix.get(x, y);
                if( output !=  null ) { 
                    x0 = x;
                    y0 = y;
                    updatePane(t);
                    //repaint(0, x, mid, mid-x, y-mid);
                    repaint();
                } else if( x0 != -1 ) {
                    repaint();
                    //repaint(0, x0, mid, mid-x0, y0-mid);
                    x0 = -1;
                    y0 = -1;
                }
 				String tooltip = "<html><font color=rgb(150,100,100) size=+2>"+"["+x+","+y+")</font>";
				if( visited!=null ) {
					int completeTime = Util.lY(visited[x][y]);
					int otherTime = Util.lX(visited[x][y]);
					tooltip += " time = <font color=rgb(100,150,100) size=+2> "+(completeTime+otherTime);
					tooltip += "</font> = <font color=rgb(150,100,100) size=+1> "+completeTime;
					tooltip += "</font> (completetion) + <font color=rgb(100,100,150)  size=+1> "+otherTime;
				}
                //if( skipped != null && 0 < skipped[x][y] )
                	//tooltip += "  <font color=rgb(100,100,150) size=+2>"+par.allSymbols[skipped[x][y]];
                setToolTipText(tooltip);
            }
			private void updatePane( final JEditorPane t ) {
				StringBuffer sb = new StringBuffer("<html><font color=red size=+1>["+x0+","+y0+")</font><br>");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				//if( par instanceof CYK )
					//updatePane4CYK(sb);
				//else if( par instanceof Earley )
					updatePane4Earley(sb);
				sb.append("<font color=green  size=+1><br><br>"); //$NON-NLS-1$
				for( int i = x0; i < y0; i++ ) {
				    sb.append(" "+src.get(i).content); // (authorized) //$NON-NLS-1$
				}
				t.setText(sb.toString());
			}
		    public void updatePane4Earley( StringBuffer sb ) {
	        	for( int j = 0; j < output.size(); j++ ) {
	        		int pos = output.getPosition(j);
	        		int ruleNo = output.getRule(j);
	        		//int mid = output.getBackpointer(j);
                    int mid = -1;
                    if( j == index )
                        mid = matrix.getEarleyBackptrs(x0, y0, output, j).get(ambig); 
	        		sb.append("<br>");
				    par.toHtml(ruleNo, pos, j==index, x0,mid,y0, matrix, sb);
				}
			}
            int x0 = -1;
            int y0 = -1;
            Cell output = null;
            int index = 0;
            int ambig = 0;
            public Dimension getPreferredScrollableViewportSize() {
                // TODO Auto-generated method stub
                return null;
            }
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
                // TODO Auto-generated method stub
                return 0;
            }
            public boolean getScrollableTracksViewportHeight() {
                // TODO Auto-generated method stub
                return false;
            }
            public boolean getScrollableTracksViewportWidth() {
                // TODO Auto-generated method stub
                return false;
            }
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                // TODO Auto-generated method stub
                return 0;
            }
            public void paint( Graphics g ) {
                super.paint(g);
                if( x0 != -1 && (
                		/*par instanceof CYK && x0+1 < y0 
                	||*/	par instanceof Earley && x0 <= y0 && index < output.size() 
                    ) ) {
                    g.setColor(Color.red);
                    int mid = -1;
                    //if( par instanceof CYK )
                        ///mid = matrix.getCykBackptrs(x0, y0, output.getSymbol(index)).get(ambig); 
                    //else if( par instanceof Earley )
                        mid = matrix.getEarleyBackptrs(x0, y0, output, index).get(ambig); 
                    if( y0 < mid ) {
                      	g.drawLine(x0*zoom+offset, (y0-1)*zoom+offset, x0*zoom+offset, y0*zoom+offset); // vertical
                      	return;
                    }
                    if( x0 <= mid )
                    	g.drawLine(x0*zoom+offset, mid*zoom+offset, x0*zoom+offset, y0*zoom+offset); // vertical
                    g.drawLine(x0*zoom+offset, y0*zoom+offset, mid*zoom+offset, y0*zoom+offset); // horiz
                }
            }
            
            /**
             * Iterate through alternative rules derivations
             */
            public void mouseWheelMoved( MouseWheelEvent e ) {
                if( output == null )
                    return;
                int ambiguityFactor = -1;//matrix.getCykBackptrs(x0, y0, output.getSymbol(index)).size();
                //if( par instanceof CYK )
                    //ambiguityFactor = matrix.getCykBackptrs(x0, y0, output.getSymbol(index)).size(); 
                //else if( par instanceof Earley )
                    ambiguityFactor = matrix.getEarleyBackptrs(x0, y0, output, index).size(); 
                
                if( 0 <= ambig + e.getWheelRotation() && ambig + e.getWheelRotation() < ambiguityFactor ) {
                    ambig += e.getWheelRotation();
                } else {
                    ambig = 0;
                
                    index += e.getWheelRotation()>0 ? 1 : -1 ;
                    if( index < 0 )
                        index = output.size()+index;
                    if( index >= output.size() )
                        index = index-output.size();
                }
                repaint();
                updatePane(t);
            }
        }
        
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel,BoxLayout.Y_AXIS));
        ButtonGroup optimGroup = new ButtonGroup();        
        optimGroup.add(yes);
        optimGroup.add(no);
        radioPanel.add(new JLabel("Optim: "));
        radioPanel.add(yes);
        radioPanel.add(no);
        //yes.setSelected(??);
        yes.addActionListener(this);
        no.addActionListener(this);
        
        
        JPanel matrixPanel = new JPanel(new BorderLayout());
        matrixImage = new ScrollablePicture();
        matrixPanel.add(matrixImage, BorderLayout.CENTER);
		matrixPanel.add(radioPanel, BorderLayout.EAST);
        
        JScrollPane canvasScrollPane = new JScrollPane(matrixPanel);
        canvasScrollPane.setVerticalScrollBarPolicy(
                                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        canvasScrollPane.setHorizontalScrollBarPolicy(
                                                      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScrollPane.setPreferredSize(new Dimension(pixels+200, pixels+100));
        canvasScrollPane.setMinimumSize(new Dimension(300, 300));

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sp.add(canvasScrollPane);
        sp.add(editorScrollPane);
        
        frame.getContentPane().add(sp);
        frame.pack();
        //frame.setSize(X, Y+350);
        frame.setVisible(true);

    }
	private BufferedImage drawMatrix( final Matrix matrix ) {
        this.matrix = matrix;
        byte[] pixels = new byte[X * Y+1]; 
        for( int j = 0; j < Y; ++j)
            for( int i = 0; i <= X; ++i) {
                int z = (j * X + i);
                Cell tmp = matrix.get(i/zoom, j/zoom);
                if( tmp!=null ) {
                    int completeTime = Util.lY(visited[i/zoom][j/zoom]);
                    int otherTime = Util.lX(visited[i/zoom][j/zoom]);
                    pixels[z] = 13;   // gray
                    if( completeTime+otherTime < 10000 )
                        pixels[z] = 14;  // light gray
                    if( 100000 < completeTime+otherTime )
                        pixels[z] = 0; // black
                    if( 1000000 < completeTime+otherTime )
                        pixels[z] = 7; // red
                } else {
					if( 
                          j < i
                      || visited != null && visited[i/zoom][j/zoom] == 0
                      || visited == null
                    ) {
                        pixels[z] = 1; // blue
                    } else {                       
                        pixels[z] = 4;  // green
                        if( visited != null ) {
                            int completeTime = Util.lY(visited[i/zoom][j/zoom]);
                            int otherTime = Util.lX(visited[i/zoom][j/zoom]);
                            if( 5000 < completeTime+otherTime )
                                pixels[z] = 3;
                        }
                    }
                }
            }
//      Create a data buffer using the byte buffer of pixel data.
        // The pixel data is not copied; the data buffer uses the byte buffer array.
        DataBuffer dbuf = new DataBufferByte(pixels, X*Y, 0);
        int numBanks = dbuf.getNumBanks(); // 1
        int bitMasks[] = new int[]{(byte)0xf};
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
                                                                   DataBuffer.TYPE_BYTE, X, Y, bitMasks);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dbuf, null);
        ColorModel colorModel = generateColorModel();
        final BufferedImage img = new BufferedImage(colorModel, raster, false, null);//new java.util.Hashtable());		
        return img;
    }

    private void drawSrcText( Graphics g ) {
        if( zoom < 5 )
            return;
        int pos = -1;
        for( LexerToken t : src ) {
            pos++;
            if( 50 < zoom )
            	g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.setColor(Color.WHITE);
            g.drawString(t.content, (pos)*zoom + 2*zoom/3, (pos+1)*zoom + 2*zoom/3);
        }
    }
    
    private static ColorModel generateColorModel() {
        // Generate 16-color model
        byte[] r = new byte[16];
        byte[] g = new byte[16];
        byte[] b = new byte[16];

        r[0] = 0; g[0] = 0; b[0] = 0;
        r[1] = 0; g[1] = 0; b[1] = (byte)192;
        r[2] = 0; g[2] = 0; b[2] = (byte)255;
        r[3] = 0; g[3] = (byte)192; b[3] = 0;
        r[4] = 0; g[4] = (byte)255; b[4] = 0;
        r[5] = 0; g[5] = (byte)192; b[5] = (byte)192;
        r[6] = 0; g[6] = (byte)255; b[6] = (byte)255;
        r[7] = (byte)192; g[7] = 0; b[7] = 0;
        r[8] = (byte)255; g[8] = 0; b[8] = 0;
        r[9] = (byte)192; g[9] = 0; b[9] = (byte)192;
        r[10] = (byte)255; g[10] = 0; b[10] = (byte)255;
        r[11] = (byte)192; g[11] = (byte)192; b[11] = 0;
        r[12] = (byte)255; g[12] = (byte)255; b[12] = 0;
        r[13] = (byte)80; g[13] = (byte)80; b[13] = (byte)80;
        r[14] = (byte)192; g[14] = (byte)192; b[14] = (byte)192;
        r[15] = (byte)255; g[15] = (byte)255; b[15] = (byte)255;

        return new IndexColorModel(4, 16, r, g, b);
    }

    private JRadioButton yes = new JRadioButton("Yes");
    private JRadioButton no = new JRadioButton("No");
	public void actionPerformed( ActionEvent e ) {
		recalculate(e.getSource() == yes);		
	}	

    public void recalculate( boolean optim ) {
        causes = new HashMap<Long,Integer>();
        if( visited != null ) {
            visited = new long[src.size()+1][src.size()+1];
                        
            ((Earley)par).skipRanges = optim;
            matrix = matrix.recalc();
            par.parse(src, matrix); 
        }
        img = drawMatrix(matrix);
        matrixImage.repaint();
    }
}
