import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.sourceforge.tess4j.TesseractException;

public class Frame {
	
	public static void main(String[] args) throws TesseractException, IOException {
					
			List<String> questions = Process.run();
			Iterator i = questions.iterator();
			
		 	JFrame f=new JFrame("Button Example");  
		    final JTextArea tf=new JTextArea();  
		    tf.setBounds(200,50, 400,300);  
		    JButton b=new JButton("Next Question");  
		    b.setBounds(350,400,95,30);  
		    b.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					tf.setText(i.next().toString());
					
				}
			});  
		    f.add(b);f.add(tf);  
		    f.setSize(800,600);  
		    f.setLayout(null);  
		    f.setVisible(true);   
		}  
	}
	
	
	//label.setIcon(imageScale(createAwtImage(originalImgMat),600,900));


