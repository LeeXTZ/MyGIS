package lyl.MyGIS;

/**
 * App类，实现程序界面、菜单、事件监听程序、功能逻辑。
 * 
 * launch()方法，用于启动程序界面；
 * setMenuBar()方法，用于定义菜单栏及其事件监听程序；
 * setToolBar()方法，定义工具栏。
 * 
 * @author
 *
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.event.MapLayerListEvent;
import org.geotools.map.event.MapLayerListListener;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.event.MapMouseListener;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class App {
	private JMapFrame mainFrame;
	private JMenuBar menuBar;
	private MapContent map;
	private ProjectCreator project;
	
	private JFileChooser chooser; // 由于JFileChooser的构造器相当耗费时间，这里声明其对象为字段，方便重用。
	
	private Transaction transaction;
	private MapMouseListener mML;
	
	public App() {
		// 设置程序界面观感
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		// 设置程序界面字体
		setUIFont (new javax.swing.plaf.FontUIResource("微软雅黑",Font.PLAIN,18));
		
		// 初始化地图、文件选择器
		map = new MapContent();
		chooser = new JFileChooser();
		chooser.setFont(new Font("微软雅黑",Font.PLAIN,18));
		
	}
	
	public static void main(String[] args) {
		App app = new App();
		setUIFont (new javax.swing.plaf.FontUIResource("微软雅黑",Font.PLAIN,18));
		// 启动程序
		app.launch();
	}
	

	private void launch() {
		mainFrame = new JMapFrame();
		// mainFrame.setTitle("MyGIS - " + project.getPrjName());
		mainFrame.setTitle("MyGIS");

		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int width = screenSize.width;
		int height = screenSize.height;
		
		mainFrame.setSize(width*4/5, height*4/5);
		
		mainFrame.enableLayerTable(true);
		mainFrame.enableStatusBar(true);
		mainFrame.enableToolBar(true);
		mainFrame.setJMenuBar(this.setMenuBar());
		//this.setMenuBar(mainFrame);
		this.setToolBar(mainFrame);
		
		mainFrame.setMapContent(map);
		mainFrame.setVisible(true);
		
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);  
		// 添加主窗口关闭事件监听
		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int option = JOptionPane.showConfirmDialog(mainFrame, "确定退出系统? ", "提示 ", JOptionPane.YES_NO_CANCEL_OPTION);
				if (option == JOptionPane.YES_OPTION) {
					if (e.getWindow() == mainFrame) {
						System.exit(0);
					} else {
						return;
					}
				}
			}
		});
		
		// 添加删除图层的事件监听
		map.addMapLayerListListener(new MapLayerListListener() {

			@Override
			public void layerRemoved(MapLayerListEvent event) {
				// TODO Auto-generated method stub
				String title = event.getLayer().getTitle();
				// 更新项目中的图层路径列表
				ArrayList<String> newPathList = new ArrayList<String>();
				for (String path : project.getLayerPathList()) {
					System.out.println(title);
					if (path.contains(title)){
						//newPathList.add(path);
						newPathList.remove(path);
						break;
					}
				}
				project.setLayerPathList(newPathList);
				System.out.println("删除图层 [" + title + "] 完成");
			}

			@Override
			public void layerAdded(MapLayerListEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void layerChanged(MapLayerListEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void layerMoved(MapLayerListEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void layerPreDispose(MapLayerListEvent event) {
				// TODO Auto-generated method stub
				
			}
			
		});
	
		setUIFont (new javax.swing.plaf.FontUIResource("微软雅黑",Font.PLAIN,18));
	}

	public static void setUIFont (javax.swing.plaf.FontUIResource f){
	    java.util.Enumeration keys = UIManager.getDefaults().keys();
	    while (keys.hasMoreElements()) {
	      Object key = keys.nextElement();
	      Object value = UIManager.get (key);
	      if (value instanceof javax.swing.plaf.FontUIResource)
	        UIManager.put (key, f);
	      }
	}

	private JMenuBar setMenuBar() {
		// create a menu bar
		menuBar = new JMenuBar();
		
		// start fileMenu
		JMenu fileMenu = new JMenu("  File  ");
		JMenuItem newProjectMenuItem = new JMenuItem("   New Project...    ", new ImageIcon("icon/新建项目.png"));
		JMenuItem openProjectMenuItem = new JMenuItem("   Open Project...    ", new ImageIcon("icon/打开项目.png"));
		JMenuItem addLayerMenuItem = new JMenuItem("   Add Layer    ", new ImageIcon("icon/添加图层.png"));
		JMenuItem saveMenuItem = new JMenuItem("   Save Project   ", new ImageIcon("icon/保存.png"));
		JMenuItem saveAsMenuItem = new JMenuItem("   Save As   ", new ImageIcon("icon/另存为.png"));
		JMenuItem exitMenuItem = new JMenuItem("   Exit    ");
		newProjectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		openProjectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		addLayerMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));	
		fileMenu.add(newProjectMenuItem);fileMenu.add("");
		fileMenu.add(openProjectMenuItem);fileMenu.add("");
		fileMenu.addSeparator();fileMenu.add("");
		fileMenu.add(addLayerMenuItem);fileMenu.add("");
		fileMenu.addSeparator();fileMenu.add("");
		fileMenu.add(saveMenuItem);fileMenu.add("");
		fileMenu.add(saveAsMenuItem);fileMenu.add("");
		fileMenu.addSeparator();fileMenu.add("");
		fileMenu.add(exitMenuItem);
		//end fileMenu
		
		//start editMenu
		JMenu editMenu = new JMenu("  Edit  ");
		
		JMenuItem startEditMenuItem = new JMenuItem("   Start Edit    ", new ImageIcon("icon/开始.png"));
		JMenuItem stopEditMenuItem = new JMenuItem("   Stop Edit    ", new ImageIcon("icon/停止.png")); stopEditMenuItem.setEnabled(false);
		JMenuItem selectFeatureMenuItem = new JMenuItem("   Select Feature    "); selectFeatureMenuItem.setEnabled(false);
		
		JMenu addFeatureMenu = new JMenu("   Add Feature...    ");
		JMenuItem addPointMenuItem = new JMenuItem("   Add Point    ", new ImageIcon("icon/添加点.png")); addPointMenuItem.setEnabled(false);
		JMenuItem addLineMenuItem = new JMenuItem("   Add Line    ", new ImageIcon("icon/添加线.png")); addLineMenuItem.setEnabled(false);
		JMenuItem addPolygonMenuItem = new JMenuItem("   Add Polygon    ", new ImageIcon("icon/添加区.png")); addPolygonMenuItem.setEnabled(false);
		addFeatureMenu.add(addPointMenuItem);
		addFeatureMenu.add(addLineMenuItem);
		addFeatureMenu.add(addPolygonMenuItem);
		addFeatureMenu.setFont(new Font("微软雅黑",Font.PLAIN,18));
		
		JMenuItem deleteFeatureMenuItem = new JMenuItem("   Delete Feature    ", new ImageIcon("icon/删除要素.png"));deleteFeatureMenuItem.setEnabled(false);
		JMenuItem copyMenuItem = new JMenuItem("   Copy    ");copyMenuItem.setEnabled(false);
		JMenuItem pasteMenuItem = new JMenuItem("   Paste    ");pasteMenuItem.setEnabled(false);
		
		editMenu.add(startEditMenuItem);
		editMenu.add(selectFeatureMenuItem);
		editMenu.add(addFeatureMenu);
		editMenu.add(deleteFeatureMenuItem);
		editMenu.add(copyMenuItem);
		editMenu.add(pasteMenuItem);
		editMenu.add(stopEditMenuItem);
		//end editMenu
		
		// start queryMenu
		JMenu queryMenu = new JMenu("  Query  ");
		JMenuItem attributeQueryMenuItem = new JMenuItem("   Attribute Query    ", new ImageIcon("icon/属性查询.png"));
		JMenuItem spatialQueryMenuItem = new JMenuItem("   Spatial Query    ", new ImageIcon("icon/空间查询.png"));
		queryMenu.add(attributeQueryMenuItem);
		queryMenu.add(spatialQueryMenuItem);
		// end queryMenu
		
		JMenu attributeMenu = new JMenu("  Attribute_Edit  ");
		JMenuItem openAttributeTableMenuItem = new JMenuItem("   Open Attribute Table   ", new ImageIcon("icon/属性表.png"));
		//JMenuItem spatialQueryMenuItem = new JMenuItem("   Spatial Query    ");
		attributeMenu.add(openAttributeTableMenuItem);
		//queryMenu.add(spatialQueryMenuItem);
		
		JMenu aboutMenu = new JMenu("  About  ");
		JMenuItem aboutUsMenuItem = new JMenuItem(" About Us ", new ImageIcon("icon/关于我们.png"));
		JMenuItem linkMenuItem = new JMenuItem(" Links ");
		aboutMenu.add(aboutUsMenuItem);
		aboutMenu.add(linkMenuItem);

		// add menu to menubar
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(queryMenu);
		menuBar.add(attributeMenu);
		menuBar.add(aboutMenu);
		
		// 使用匿名类，定义事件监听
		// 【新建项目】菜单项事件监听
		newProjectMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				newProjectPerformer();
			}

		});
		// 【打开项目】菜单项事件监听
		openProjectMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				openProjectPerformer();
			}

		});
		// 【添加图层】菜单项事件监听
		addLayerMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				addLayerPerformer();
			}
		});

		// 【保存】菜单项事件监听
		saveMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				saveProjectPerformer();
			}

		});
		// 【另存为】菜单项事件监听
		saveAsMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				saveAsProjectPerformer();
			}

		});
		
		// 【退出】菜单项事件监听
		exitMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				mainFrame.dispatchEvent(new WindowEvent(mainFrame,WindowEvent.WINDOW_CLOSING) );
			}
			
		});
		
		// 【开始编辑】菜单项事件监听
		startEditMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				startEditMenuItem.setEnabled(false);
				stopEditMenuItem.setEnabled(true);
				addPointMenuItem.setEnabled(true);
				addLineMenuItem.setEnabled(true);
				addPolygonMenuItem.setEnabled(true);
				selectFeatureMenuItem.setEnabled(true);
				deleteFeatureMenuItem.setEnabled(true);
				
				startEditPerformer();
			}
			
		});
		// 【停止编辑】菜单项事件监听
		stopEditMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				startEditMenuItem.setEnabled(true);
				stopEditMenuItem.setEnabled(false);
				addPointMenuItem.setEnabled(false);
				addLineMenuItem.setEnabled(false);
				addPolygonMenuItem.setEnabled(false);
				selectFeatureMenuItem.setEnabled(false);
				deleteFeatureMenuItem.setEnabled(false);
				
				stopEditPerformer();
			}
			
		});
		// 【选择要素】菜单项事件监听
		selectFeatureMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				selectFeaturePerformer();
			}
			
		});
		
		// 【添加点要素】菜单项事件监听
		addPointMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				addPointPerformer();
			}
			
		});
		// 【添加线要素】菜单项事件监听
		addLineMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				addLinePerformer();
			}
			
		});
		// 【添加面要素】菜单项事件监听
		addPolygonMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				addPolyPerformer();
			}
			
		});
		// 【删除要素】 菜单项事件监听
		deleteFeatureMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				deleteFeaturePerformer();
			}
			
		});
		
		// 【属性查询】菜单项事件监听
		attributeQueryMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				attributeQueryPerformer();
			}
			
		});
		// 【空间查询】菜单项事件监听
		spatialQueryMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				spatialQueryPerformer();
			}
			
		});
		// 【打开属性表】菜单项事件监听
		openAttributeTableMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				openAttributeTablePerformer();
			}
			
		});
		
		// 【关于我们】菜单项事件监听
		aboutUsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				String message = "Version: 0.0.1 Release\r\n" + 
						"Build id: 20171231-2134\r\n" + 
						"\r\n" + 
						"(c) Copyright MyGIS contributors and others \r\n" + 
						"All rights reserved. \r\n" +
						"2017~2017.  ";
				String title = "About MyGIS";
				JOptionPane.showMessageDialog(mainFrame, message, title, JOptionPane.INFORMATION_MESSAGE);
			}
			
		});
		// 【链接】菜单项事件监听
		linkMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JOptionPane.showMessageDialog(mainFrame, "NO LINK.");
			}
			
		});
		return menuBar;
		
	}

	private void setToolBar(JMapFrame mainFrame) {
		JToolBar toolBar = mainFrame.getToolBar();
		
		// Action
		class OpenPrjAction extends AbstractAction {
		    public OpenPrjAction(String text, ImageIcon icon, String desc) {
		        super(text, icon);
		        putValue(SHORT_DESCRIPTION, desc);
		        //putValue(MNEMONIC_KEY, mnemonic);
		    }
		    public void actionPerformed(ActionEvent e) {
		    	openProjectPerformer();
		    }
		}
		class NewPrjAction extends AbstractAction {
		    public NewPrjAction(String text, ImageIcon icon, String desc) {
		        super(text, icon);
		        putValue(SHORT_DESCRIPTION, desc);
		        //putValue(MNEMONIC_KEY, mnemonic);
		    }
		    public void actionPerformed(ActionEvent e) {
		    	newProjectPerformer();
		    }
		}
		class SaveAction extends AbstractAction {
		    public SaveAction(String text, ImageIcon icon, String desc) {
		        super(text, icon);
		        putValue(SHORT_DESCRIPTION, desc);
		        //putValue(MNEMONIC_KEY, mnemonic);
		    }
		    public void actionPerformed(ActionEvent e) {
		    	saveProjectPerformer();
		    }
		}
		class AddLayerAction extends AbstractAction {
		    public AddLayerAction(String text, ImageIcon icon, String desc) {
		        super(text, icon);
		        putValue(SHORT_DESCRIPTION, desc);
		        //putValue(MNEMONIC_KEY, mnemonic);
		    }
		    public void actionPerformed(ActionEvent e) {
		    	addLayerPerformer();
		    }
		}
		OpenPrjAction openPrjAction = new OpenPrjAction("", new ImageIcon("icon/打开项目.png"), "打开项目");
		NewPrjAction newPrjAction = new NewPrjAction("", new ImageIcon("icon/新建项目.png"), "新建项目");
		SaveAction saveAction = new SaveAction("", new ImageIcon("icon/保存.png"), "保存项目");
		AddLayerAction adLayerAction = new AddLayerAction("", new ImageIcon("icon/添加图层2.png"), "添加图层");
		
		// Button
		JButton openPrjBtn = new JButton(openPrjAction);
		JButton newPrjBtn = new JButton(newPrjAction);
		JButton savePrjBtn = new JButton(saveAction);
		JButton addLayerBtn = new JButton(adLayerAction); 
		
		// Add Button
		Component[] cpnts = toolBar.getComponents();
		toolBar.removeAll();
		for(int i = 0; i <5; i++) {
			toolBar.addSeparator(new Dimension(3,20));
		}
		toolBar.add(openPrjBtn);
		toolBar.add(newPrjBtn);
		toolBar.add(savePrjBtn);
		toolBar.addSeparator(new Dimension(10,20));
		toolBar.add(addLayerBtn);
		toolBar.addSeparator(new Dimension(10,20));
		for (Component cpnt : cpnts) {
			toolBar.add(cpnt);
		}
		
	}
	
	protected void newProjectPerformer() {

		// 如果存在当前项目
		if (this.project != null) {

			int selection = JOptionPane.showConfirmDialog(mainFrame, "是否保存当前项目？", "保存", JOptionPane.YES_NO_CANCEL_OPTION);
			if (selection == JOptionPane.CANCEL_OPTION || selection == JOptionPane.CLOSED_OPTION) {
				return;
			} else if (selection == JOptionPane.YES_OPTION) {
				project.savePrjFile();
				JOptionPane.showMessageDialog(chooser, "保存项目完成！", null, JOptionPane.INFORMATION_MESSAGE);
				/*
				 * // 如果当前项目保存过，则保存到已存在的项目文件。 if (project.getIsSaved()) { project.savePrjFile();
				 * // 如果当前项目未保存过，则新建项目文件并保存 } else {
				 * 
				 * // 设置当前目录为当前的工作目录 chooser.setCurrentDirectory(new File("."));
				 * chooser.setSelectedFile(new File(project.getPrjName() + ".mgprj"));
				 * 
				 * // 设置一个过滤器，使只能保存为.mgprj文件 chooser.resetChoosableFileFilters();
				 * chooser.setFileFilter(new FileNameExtensionFilter("MyGIS Project Files",
				 * ".mgprj"));
				 * 
				 * int saveResult = chooser.showSaveDialog(mainFrame); if (saveResult ==
				 * JFileChooser.APPROVE_OPTION) { File afile = chooser.getSelectedFile(); if
				 * (!afile.getName().toLowerCase().endsWith(".mgprj")) { afile = new
				 * File(afile.getAbsoluteFile() + ".mgprj"); } project.newPrjFile(afile);
				 * JOptionPane.showMessageDialog(chooser, "保存完成！", null,
				 * JOptionPane.INFORMATION_MESSAGE); } else { return; } }
				 */
			}

			// 如果不存在当前项目
		} else {
			// pass
		}
		this.project = new ProjectCreator();
		// 设置当前目录为当前的工作目录
		chooser.setCurrentDirectory(new File("."));
		chooser.setSelectedFile(new File("newProject" + project.getPrjNumber() + ".mgprj"));

		// 设置一个过滤器，使只能保存为.mgprj文件
		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(new FileNameExtensionFilter("MyGIS项目文件(*.mgprj)", "mgprj"));

		// int saveResult = chooser.showSaveDialog(mainFrame);
		chooser.setDialogTitle("新建项目");
		int result = chooser.showDialog(mainFrame, "确定");
		if (result == JFileChooser.APPROVE_OPTION) {
			File afile = chooser.getSelectedFile();
			if (!afile.getName().toLowerCase().endsWith(".mgprj")) {
				afile = new File(afile.getAbsoluteFile() + ".mgprj");
			}
			project.newPrjFile(afile);

			String prjNameWithoutExtension = null;
			if (afile.getName().endsWith(".mgprj")) {
				prjNameWithoutExtension = afile.getName().substring(0, afile.getName().length() - 6);
			} else {
				prjNameWithoutExtension = afile.getName();
			}
			project.setPrjName(prjNameWithoutExtension);
			JOptionPane.showMessageDialog(chooser, "新建项目完成！", null, JOptionPane.INFORMATION_MESSAGE);
			System.out.println("新建项目 " + prjNameWithoutExtension + " 完成");
		} else {
			this.project = null;
			return;
		}

		////////////////////////// 新建项目必备... ////////////////////////////////////////
		for (Layer layer : map.layers())
			map.removeLayer(layer);
		// map = new MapContent();
		mainFrame.setMapContent(map);
		// project = new Project();

		mainFrame.setTitle("MyGIS - " + project.getPrjName());
		//////////////////////////////////////////////////////////////////////////////////
	}

	protected void openProjectPerformer() {
		if (this.project != null) {
			int selection = JOptionPane.showConfirmDialog(mainFrame, "打开新项目之前，是否保存当前项目？", "保存", JOptionPane.YES_NO_CANCEL_OPTION);
			if (selection == JOptionPane.YES_OPTION) {

				/*
				 if (project.getIsSaved()) { // 如果当前项目保存过，则保存到已存在的项目文件。
				 
				 project.savePrjFile();
				 
				 } else { // 如果当前项目未保存过，则新建项目文件并保存
				 
				 chooser.setCurrentDirectory(new File(".")); // 设置当前目录为当前的工作目录
				 chooser.setSelectedFile(new File(project.getPrjName() + ".mgprj"));
				 
				 ///////////////////////////// 设置一个过滤器，使只能保存为.mgprj文件
				 /////////////////////////// chooser.resetChoosableFileFilters();
				 chooser.setFileFilter(new FileNameExtensionFilter("MyGIS项目文件", "mgprj"));
				 
				 int saveResult = chooser.showSaveDialog(mainFrame); if (saveResult ==
				 JFileChooser.APPROVE_OPTION) { File afile = chooser.getSelectedFile(); if
				 (!afile.getName().toLowerCase().endsWith(".mgprj")) { afile = new
				 File(afile.getAbsolutePath() + ".mgprj"); } project.newPrjFile(afile);
				 JOptionPane.showMessageDialog(chooser, "保存完成！", null,
				 JOptionPane.INFORMATION_MESSAGE);
				 /////////////////////////////////////////////////////////////////////////////
				 //////////////// } else { return; } }
				 */

				project.savePrjFile();
				JOptionPane.showMessageDialog(chooser, "保存项目完成！", null, JOptionPane.INFORMATION_MESSAGE);

			} else if (selection == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}
		chooser.setCurrentDirectory(new File(".")); // 设置当前目录为当前的工作目录
		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(new FileNameExtensionFilter("MyGIS项目文件(*.mgprj)", "mgprj"));

		chooser.setDialogTitle("打开项目");
		int openResult = chooser.showOpenDialog(mainFrame);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			// 清除当前图层
			for (Layer layer : map.layers())
				map.removeLayer(layer);

			// 读取项目文件，创建项目
			project = new ProjectCreator(chooser.getSelectedFile());

			// 将layerPathList中的图层添加到地图中
			ArrayList<String> pathList = project.getLayerPathList();
			ArrayList<Layer> layerList = new ArrayList<Layer>();
			if (pathList != null && !pathList.isEmpty()) {
				for (String i : pathList) {
					File f = new File(i);
					//this.addLayer(f);  //直接调用addLayer方法，会报错：“找不到.shx文件”
					// 判断图层类型
					Layer l;
					if (f.getName().toLowerCase().endsWith(".shp")) {
						l = this.getLayerFromShp(f);
					}else {
						l = this.getLayerFromRaster(f);
					}
					
					layerList.add(l);
				}
			}
			map.addLayers(layerList);

			//mainFrame.setMapContent(map);
			mainFrame.setTitle("MyGIS - " + project.getPrjName());
			System.out.println("打开项目 " + project.getPrjName() + " 完成");

		}

	}

	protected void saveProjectPerformer() {
		int selection = JOptionPane.showConfirmDialog(mainFrame, "确定保存当前项目？", "保存", JOptionPane.YES_NO_CANCEL_OPTION);
			if (selection == JOptionPane.YES_OPTION) {
			project.savePrjFile();
			JOptionPane.showMessageDialog(chooser, "保存项目完成！", null, JOptionPane.INFORMATION_MESSAGE);
			System.out.println("保存项目 " + project.getPrjName() + " 完成");
		}
	}

	protected void saveAsProjectPerformer() {
		// 设置当前目录为当前的工作目录
		chooser.setCurrentDirectory(new File("."));
		chooser.setSelectedFile(new File(project.getPrjName() + "0.mgprj"));

		// 设置一个过滤器，使只能保存为.mgprj文件
		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(new FileNameExtensionFilter("MyGIS项目文件(*.mgprj)", "mgprj"));

		chooser.setDialogTitle("另存为项目");
		int result = chooser.showDialog(mainFrame, "确定");
		if (result == JFileChooser.APPROVE_OPTION) {
			File afile = chooser.getSelectedFile();
			if (!afile.getName().toLowerCase().endsWith(".mgprj")) {
				afile = new File(afile.getAbsoluteFile() + ".mgprj");
			}

			// 设置项目名为用户在对话框中的输入
			String prjNameWithoutExtension = null;
			if (afile.getName().endsWith(".mgprj")) {
				prjNameWithoutExtension = afile.getName().substring(0, afile.getName().length() - 6);
			} else {
				prjNameWithoutExtension = afile.getName();
			}
			project.setPrjName(prjNameWithoutExtension);

//			// 更新项目中的图层路径列表
//			ArrayList<String> newPathList = new ArrayList<String>();
//			for (String path : project.getLayerPathList()) {
//				for (Layer layer : map.layers()) {
//					if (path.contains(layer.getTitle())) {
//						newPathList.add(path);
//						break;
//					}
//				}
//			}
//			project.setLayerPathList(newPathList);

			project.newPrjFile(afile);
			JOptionPane.showMessageDialog(chooser, "另存为项目完成！", null, JOptionPane.INFORMATION_MESSAGE);
			System.out.println("另存为项目 " + prjNameWithoutExtension + " 完成");
		}
	}

	protected void addLayerPerformer() {
		
		chooser.setCurrentDirectory(new File(".")); // 设置当前目录为当前的工作目录

		chooser.setSelectedFile(new File(""));
		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(new FileNameExtensionFilter(
				"Shape files or Raster files", "shp", "jpg", "jpeg", "png", "tiff"));

		chooser.setDialogTitle("添加图层");
		int addResult = chooser.showOpenDialog(mainFrame);
		if (addResult == JFileChooser.APPROVE_OPTION) {
			File afile = chooser.getSelectedFile();
			//addLayer(afile);
			// 判断图层类型
			Layer layer;
			if (afile.getName().toLowerCase().endsWith(".shp")) {
				layer = this.getLayerFromShp(afile);
			}else {
				layer = this.getLayerFromRaster(afile);
			}
			map.addLayer(layer);

			// 将图层路径添加到当前项目的图层路径列表
			ArrayList<String> layerPathList = project.getLayerPathList();
			if(!layerPathList.contains(afile.getAbsolutePath())) {
				layerPathList.add(afile.getAbsolutePath());
			}

			project.setLayerPathList(layerPathList);
			
			System.out.println("添加图层 [" + layer.getTitle() + "] 完成");
		} else {
			return;
		}
		
	}

	protected void startEditPerformer() {
		transaction = new DefaultTransaction("create");
	}
	protected void stopEditPerformer() {
		if(transaction != null) {
			try { transaction.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		// 恢复光标样式
		mainFrame.getMapPane().setCursor(Cursor.getDefaultCursor());
		// 去除鼠标事件监听
		if (mML != null) {
			mainFrame.getMapPane().removeMouseListener(mML);
		}
	}

	protected void selectFeaturePerformer() {
		removeMML();
		
		Layer editLayer = null;
		for (Layer layer :map.layers()) {
			if(layer.isSelected()) {
				//editable = true;
				editLayer = layer;
				break;
			}
		}
		if (editLayer == null) {
			JOptionPane.showMessageDialog(mainFrame, "当前不存在被选中图层，无法编辑！", "", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final Layer innerEditLayer = editLayer;
		mML = new MapMouseAdapter() {
			public void onMouseClicked(MapMouseEvent ev) {
				SelectionCreator sc = new SelectionCreator(mainFrame, innerEditLayer);
				sc.selectFeatures(ev);
			}
		};
		
		mainFrame.getMapPane().addMouseListener(mML);
	}

	protected void addPointPerformer() {
		// 去除之前的鼠标事件监听
		removeMML();
		mainFrame.getMapPane().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		CoordinateReferenceSystem mapCRS = map.getCoordinateReferenceSystem();
		//final SimpleFeatureType POINT_TYPE = createPointFeatureType(mapCRS);

		mML = new MapMouseAdapter() {

			public void onMouseClicked(MapMouseEvent ev) {
				int selection = JOptionPane.showConfirmDialog(mainFrame, "确定添加此点？");
				if(selection == JOptionPane.OK_OPTION) {
					DirectPosition2D clickPos = ev.getWorldPos();
					clickPos.setCoordinateReferenceSystem(mapCRS);
					
					GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
					com.vividsolutions.jts.geom.Point point = geometryFactory.createPoint(new Coordinate(clickPos.x, clickPos.y));
					
					SimpleFeatureType pointType = createFeatureType(mapCRS, 0);
					SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(pointType);
					featureBuilder.add(point);
					SimpleFeature feature = featureBuilder.buildFeature(null);
					
					ArrayList<SimpleFeature> features = new ArrayList<>();
	                features.add(feature);
	
					for (Layer layer : map.layers()) {
						if(layer.isSelected() && layer.isVisible()) {
							SimpleFeatureSource layerFeatureSource = (SimpleFeatureSource) layer.getFeatureSource();
							
							if (layerFeatureSource instanceof SimpleFeatureStore) {
					            SimpleFeatureStore featureStore = (SimpleFeatureStore) layerFeatureSource;
					            SimpleFeatureCollection collection = new ListFeatureCollection(pointType, features);
					            //Transaction transaction = new DefaultTransaction("create");
					            //transaction = new DefaultTransaction("create");
					            featureStore.setTransaction(transaction);
					            try {
					                featureStore.addFeatures(collection);
					                transaction.commit();
					                //mainFrame.getMapPane().repaint();
					                layer.setVisible(false);
					                layer.setVisible(true);
					                //map.removeLayer(layer);
					                //map.addLayer(layer);
					                System.out.println("添加点  (" + clickPos.x + "," + clickPos.y + ") 完成");
					            } catch (Exception problem) {
					                problem.printStackTrace();
									try { transaction.rollback(); } catch (IOException e) { e.printStackTrace(); }
					            }
					        } else {
					            System.out.println("this layer does not support read/write access");
					        }
							break;
						}
					}
				}
			}
		};
		mainFrame.getMapPane().addMouseListener(mML);
	}

	protected void addLinePerformer() {
		// 去除之前的鼠标事件监听
		removeMML();
		// 设置光标形状为十字
		mainFrame.getMapPane().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		CoordinateReferenceSystem mapCRS = map.getCoordinateReferenceSystem();
		
		mML = new MapMouseAdapter() {
			boolean state = false; // false：绘制结束或绘制未开始； true：处于绘制过程中
			int currx = 0;
			int curry = 0;
			int startx, starty, endx, endy; //起始屏幕坐标与终点屏幕坐标
			DirectPosition2D startPos, endPos; //地图坐标
			
			public void onMouseClicked(MapMouseEvent ev) {
				
				if (SwingUtilities.isLeftMouseButton(ev)) {
					state = true;
					startx = ev.getX();
					starty = ev.getY();
					
					startPos = ev.getWorldPos();
				}
				
				if (SwingUtilities.isRightMouseButton(ev)) {
					if(state) { 
						state = false;
						endx = ev.getX();
						endy = ev.getY();
						mainFrame.getMapPane().getGraphics().drawLine(startx, starty, endx, endy);
						//mainFrame.getMapPane().repaint();
						endPos = ev.getWorldPos();
						
						// 向图层添加线要素
						int selection = JOptionPane.showConfirmDialog(mainFrame, "确定添加此线？");
						if(selection == JOptionPane.OK_OPTION) {
							//DirectPosition2D clickPos = e.getWorldPos();
							startPos.setCoordinateReferenceSystem(mapCRS);
							endPos.setCoordinateReferenceSystem(mapCRS);
							Coordinate[] coordinates = {new Coordinate(startPos.x, startPos.y), new Coordinate(endPos.x, endPos.y)};
							
							// 添加线要素待写
							GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
							//Point point = geometryFactory.createPoint(new Coordinate(clickPos.x, clickPos.y));
							LineString line = geometryFactory.createLineString(coordinates);
							
							SimpleFeatureType lineType = createFeatureType(mapCRS, 1);
							SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(lineType);
							featureBuilder.add(line);
							SimpleFeature feature = featureBuilder.buildFeature(null);
							
							ArrayList<SimpleFeature> features = new ArrayList<>();
			                features.add(feature);
			                
			                for (Layer layer : map.layers()) {
								if(layer.isSelected() && layer.isVisible()) {
									SimpleFeatureSource layerFeatureSource = (SimpleFeatureSource) layer.getFeatureSource();
									
									if (layerFeatureSource instanceof SimpleFeatureStore) {
							            SimpleFeatureStore featureStore = (SimpleFeatureStore) layerFeatureSource;
							            SimpleFeatureCollection collection = new ListFeatureCollection(lineType, features);
							            
							            featureStore.setTransaction(transaction);
							            try {
							                featureStore.addFeatures(collection);
							                transaction.commit();
							                layer.setVisible(false);
							                layer.setVisible(true);
							                System.out.println("添加线  {(" + startPos.x + "," + startPos.y + "), " + 
							                						   "(" + endPos.x + "," + endPos.y + ")}" + " 完成");
							                
							            } catch (Exception problem) {
							                problem.printStackTrace();
											try { transaction.rollback(); } catch (IOException e) { e.printStackTrace(); }
							            }
							        } else {
							            System.out.println("this layer does not support read/write access");
							        }
									break;
								}
							}
						}
					}
				}
				
			}

			public void onMouseMoved(MapMouseEvent e) {
				if (state) {
					currx = e.getX();
					curry = e.getY();
					
					mainFrame.getMapPane().getGraphics().drawLine(startx, starty, currx, curry);
					mainFrame.getMapPane().repaint();

				}
			}
			
		};
		mainFrame.getMapPane().addMouseListener(mML);
	}

	protected void addPolyPerformer() {
		// 去除之前的鼠标事件监听
		removeMML();
		// 设置光标形状为十字
		mainFrame.getMapPane().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		CoordinateReferenceSystem mapCRS = map.getCoordinateReferenceSystem();
		
		mML = new MapMouseAdapter() {
			boolean state = false; // false：未处于绘制状态； true：处于绘制状态

			ArrayList<java.awt.Point> points = new ArrayList<java.awt.Point>();  //用于存储多边形各点的屏幕坐标
			java.awt.Point startPoint, currPoint, endPoint;
			ArrayList<DirectPosition2D> positions = new ArrayList<DirectPosition2D>();  //用于存储多边形各点的地图坐标
			
			
			public void onMousePressed(MapMouseEvent ev) {
				
				if (SwingUtilities.isLeftMouseButton(ev)) {
					state = true;
					if (points.size() == 0) {
						startPoint = ev.getPoint();
					}
					points.add(ev.getPoint());
					
					positions.add(ev.getWorldPos());
				}
				
				if (SwingUtilities.isRightMouseButton(ev)) {
					if(state) { 
						state = false;
						endPoint = ev.getPoint();
						points.add(endPoint);
						positions.add(ev.getWorldPos());
						
						for (int i = 0; i < points.size()-1; i++) {
							mainFrame.getMapPane().getGraphics().drawLine(points.get(i).x, points.get(i).y, points.get(i+1).x, points.get(i+1).y);
						}
						java.awt.Point lastPoint = points.get(points.size()-1);
						mainFrame.getMapPane().getGraphics().drawLine(lastPoint.x, lastPoint.y, endPoint.x, endPoint.y);
						mainFrame.getMapPane().getGraphics().drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
						
						// 将多边形要素添加到图层
						int selection = JOptionPane.showConfirmDialog(mainFrame, "确定添加此多边形要素？");
						if(selection == JOptionPane.OK_OPTION) {
							ArrayList<Coordinate> coordinates_al = new ArrayList<Coordinate>();
							for (DirectPosition2D position : positions) {
								position.setCoordinateReferenceSystem(mapCRS);
								coordinates_al.add(new Coordinate(position.x, position.y));
							}
							// 将第一个点的坐标添加到序列末尾，以形成闭合点序列
							coordinates_al.add(coordinates_al.get(0));
							// 转换成数组
							Coordinate[] coordinates = new Coordinate[coordinates_al.size()];
							coordinates_al.toArray(coordinates);
							
							// 添加多边形要素
							GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
							//Point point = geometryFactory.createPoint(new Coordinate(clickPos.x, clickPos.y));
							
							com.vividsolutions.jts.geom.Polygon polygon = geometryFactory.createPolygon(coordinates);
							
							SimpleFeatureType polygonType = createFeatureType(mapCRS, 2);
							SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(polygonType);
							featureBuilder.add(polygon);
							SimpleFeature feature = featureBuilder.buildFeature(null);
							
							ArrayList<SimpleFeature> features = new ArrayList<>();
			                features.add(feature);
			                
			                for (Layer layer : map.layers()) {
								if(layer.isSelected() && layer.isVisible()) {
									SimpleFeatureSource layerFeatureSource = (SimpleFeatureSource) layer.getFeatureSource();
									
									if (layerFeatureSource instanceof SimpleFeatureStore) {
							            SimpleFeatureStore featureStore = (SimpleFeatureStore) layerFeatureSource;
							            SimpleFeatureCollection collection = new ListFeatureCollection(polygonType, features);
							            
							            featureStore.setTransaction(transaction);
							            try {
							                featureStore.addFeatures(collection);
							                transaction.commit();
							                layer.setVisible(false);
							                layer.setVisible(true);
							                System.out.println("添加区完成");
							                
							            } catch (Exception problem) {
							                problem.printStackTrace();
											try { transaction.rollback(); } catch (IOException e) { e.printStackTrace(); }
							            }
							        } else {
							            System.out.println("this layer does not support read/write access");
							        }
									break;
								}
							}
						}
						
					}
					// 多边形绘制添加结束，清空点序列
					points.clear();
					positions.clear();
				}
			
			}

			public void onMouseMoved(MapMouseEvent ev) {
				// 如果处于绘制状态
				if (state) {
					// 获得当前点
					currPoint = ev.getPoint();
					
					for (int i = 0; i < points.size()-1; i++) {
						mainFrame.getMapPane().getGraphics().drawLine(points.get(i).x, points.get(i).y, points.get(i+1).x, points.get(i+1).y);
					}
					java.awt.Point lastPoint = points.get(points.size()-1);
					mainFrame.getMapPane().getGraphics().drawLine(lastPoint.x, lastPoint.y, currPoint.x, currPoint.y);
					mainFrame.getMapPane().getGraphics().drawLine(startPoint.x, startPoint.y, currPoint.x, currPoint.y);
					mainFrame.getMapPane().repaint();
					
					state = true;
				}
			}
		};
		
		mainFrame.getMapPane().addMouseListener(mML);
	}
	
	protected void deleteFeaturePerformer() {
		// 去除之前的鼠标事件监听
		removeMML();
		// 设置光标形状为默认
		mainFrame.getMapPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		Layer editLayer = null;
		for (Layer layer :map.layers()) {
			if(layer.isSelected()) {
				//editable = true;
				editLayer = layer;
				break;
			}
		}
		if (editLayer == null) {
			JOptionPane.showMessageDialog(mainFrame, "当前不存在被选中图层，无法编辑！", "", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final Layer innerEditLayer = editLayer;
		mML = new MapMouseAdapter() {
			public void onMouseClicked(MapMouseEvent ev) {
				SelectionCreator sc = new SelectionCreator(mainFrame, innerEditLayer);
				Set<FeatureId> IDs = sc.selectFeatures(ev);
				// 删除选中要素 待实现
				Iterator<FeatureId> iter = IDs.iterator();
				while(iter.hasNext()) {
					try {
						FeatureId featureId = iter.next();
						//String cql = "FID = '" + FeatureIdentifier + "'";
						//FeatureIdentifier = 'bc_voting_areas.7987'
						//Filter filter = CQL.toFilter(cql);
						FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	
						Filter filter = ff.id(featureId);
						SimpleFeatureStore featureStore = (SimpleFeatureStore) innerEditLayer.getFeatureSource();
						int sln = JOptionPane.showConfirmDialog(mainFrame, "确定删除选中要素？");
						if(sln == JOptionPane.OK_OPTION) {
				            featureStore.setTransaction(transaction);
							featureStore.removeFeatures(filter);
							transaction.commit();
							
							System.out.println("删除要素" + featureId + "完成");
						}
						innerEditLayer.setVisible(false);
						innerEditLayer.setVisible(true);
			            
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		
		mainFrame.getMapPane().addMouseListener(mML);
	}

	protected void attributeQueryPerformer() {
		QueryCreator query = new QueryCreator();
		query.attributeQuery(mainFrame);
	}
	protected void spatialQueryPerformer() {
		QueryCreator query = new QueryCreator();
		
	    // queryDialog
		JDialog spatialQueryDialog = new JDialog(mainFrame, "空间查询", false);
    	Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int width = screenSize.width;
		int height = screenSize.height;
		spatialQueryDialog.setSize(width * 5 / 16, height * 1 / 5);

		spatialQueryDialog.setLocationRelativeTo(mainFrame);
		spatialQueryDialog.setLocation(280, 110);
		
    	spatialQueryDialog.setLayout(new BorderLayout());
    	
    	JPanel panel = new JPanel();
    	spatialQueryDialog.add(panel, BorderLayout.NORTH);
    	
    	// layerPanel
    	JPanel layerPanel = new JPanel();
    	
        JLabel layerLabel = new JLabel("选择查询图层：", JLabel.LEFT);
        JComboBox<Layer> layerCombo = new JComboBox<>();
        for(Layer layer : map.layers()) {
        	layerCombo.addItem(layer);
        }
        
        layerPanel.add(layerLabel);
        layerPanel.add(layerCombo);
        spatialQueryDialog.add(layerPanel, BorderLayout.CENTER);
        
        // buttonPanel
    	JPanel buttonPanel = new JPanel();
    	
        JButton startQueryBtn = new JButton("开始查询");
        JButton stopQueryBtn = new JButton("结束查询");
        buttonPanel.add(startQueryBtn);
        buttonPanel.add(stopQueryBtn);
        
        spatialQueryDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        //【开始查询】按钮的点击事件监听
        startQueryBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 去除之前的鼠标事件监听
				removeMML();
				// 设置光标形状为十字
				mainFrame.getMapPane().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				CoordinateReferenceSystem mapCRS = map.getCoordinateReferenceSystem();
				Layer qLayer = layerCombo.getItemAt(layerCombo.getSelectedIndex());
				
				mML = new MapMouseAdapter() {
					boolean state = false; // false：绘制结束或绘制未开始； true：处于绘制过程中
					int currx = 0;
					int curry = 0;
					int NWx, NWy, SEx, SEy; //起始屏幕坐标与终点屏幕坐标
					DirectPosition2D NWPos, SEPos; //地图坐标
					
					public void onMouseClicked(MapMouseEvent ev) {
						// 如果点击的是鼠标左键
						if (SwingUtilities.isLeftMouseButton(ev)) {
							state = true;
							NWx = ev.getX();
							NWy = ev.getY();
							
							NWPos = ev.getWorldPos();
						}
						// 如果点击的是鼠标右键
						if (SwingUtilities.isRightMouseButton(ev)) {
							if(state) { 
								state = false;
								SEx = ev.getX();
								SEy = ev.getY();
								mainFrame.getMapPane().getGraphics().drawLine(NWx, NWy, SEx, NWy);
								mainFrame.getMapPane().getGraphics().drawLine(NWx, NWy, NWx, SEy);
								mainFrame.getMapPane().getGraphics().drawLine(SEx, NWy, SEx, SEy);
								mainFrame.getMapPane().getGraphics().drawLine(NWx, SEy, SEx, SEy);
								//mainFrame.getMapPane().repaint();
								SEPos = ev.getWorldPos();
								
								// 查询所选区域
								int selection = JOptionPane.showConfirmDialog(mainFrame, "查询此区域？");
								if(selection == JOptionPane.OK_OPTION) {
									//DirectPosition2D clickPos = e.getWorldPos();
									NWPos.setCoordinateReferenceSystem(mapCRS);
									SEPos.setCoordinateReferenceSystem(mapCRS);
									
									// 查询具体实现
									
									QueryCreator q = new QueryCreator();
									try {
										SimpleFeatureCollection features = q.spatialQuery(qLayer, NWPos, SEPos);
										
										//将查询到的要素显示到一个表格中
										JTable table = new JTable();
								        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
								        table.setModel(new DefaultTableModel(5, 5));
								        table.setPreferredScrollableViewportSize(new Dimension(500, 200));
								        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
								        table.setModel(model);
								        
								        JScrollPane scrollPane = new JScrollPane(table);
								        JDialog queryResultDialog = new JDialog(mainFrame, "空间查询结果", false);
								        queryResultDialog.setSize(1000, 500);
								        queryResultDialog.setLocationRelativeTo(null);
								        
								        queryResultDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
										
								        queryResultDialog.setVisible(true);
									} catch (CQLException | IOException e) {
										e.printStackTrace();
									}
									
								}
							}
						}
						
					}

					public void onMouseMoved(MapMouseEvent e) {
						if (state) {
							currx = e.getX();
							curry = e.getY();
							
							mainFrame.getMapPane().getGraphics().drawLine(NWx, NWy, currx, NWy);
							mainFrame.getMapPane().getGraphics().drawLine(NWx, NWy, NWx, curry);
							mainFrame.getMapPane().getGraphics().drawLine(currx, NWy, currx, curry);
							mainFrame.getMapPane().getGraphics().drawLine(NWx, curry, currx, curry);
							
							mainFrame.getMapPane().repaint();

						}
					}
					
				};
				mainFrame.getMapPane().addMouseListener(mML);
			}
        	
        });
        
        //【结束查询】按钮的点击事件监听
        stopQueryBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				// 去除之前的鼠标事件监听
				removeMML();
				// 设置光标形状为十字
				mainFrame.getMapPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				
			}
        	
        });
        
        spatialQueryDialog.setVisible(true);
	}

	protected void openAttributeTablePerformer() {
		
		Layer editLayer = null;
		for (Layer layer : map.layers()) {
			if(layer.isSelected() && layer.isVisible()) {
				editLayer = layer;
				break;
			}
		}
		if (editLayer != null) {
			SimpleFeatureSource source = (SimpleFeatureSource) editLayer.getFeatureSource();
			final SimpleFeatureStore store = (SimpleFeatureStore) source;
			Filter filter;
			try {
				//查询所有要素的所有属性
				filter = CQL.toFilter("include");
				SimpleFeatureCollection features = source.getFeatures(filter);
				
				// 将属性显示到表格中
				JTable table = new JTable();
		        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		        table.setPreferredScrollableViewportSize(new Dimension(500, 200));
		        
		        // 定义不可编辑的table model
		        FeatureCollectionTableModel uneditableModel = new FeatureCollectionTableModel(features);
		        table.setModel(uneditableModel);
		        JScrollPane scrollPane = new JScrollPane(table);
		        
		        // 定义属性表窗口的菜单栏
		        JMenu attributeMenu = new JMenu(" 属性编辑 ");
		        JMenuItem startEditItem = new JMenuItem(" 开始编辑 ");
		        startEditItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						 // 定义可编辑的table model
				        EditableModel editableModel = new EditableModel(uneditableModel);
				        editableModel.addTableModelListener(new TableModelListener() {

							@Override
							public void tableChanged(TableModelEvent e) {
								// TODO Auto-generated method stub
								if (e.getType() == TableModelEvent.UPDATE) {
									int row = e.getFirstRow();  
							        int column = e.getColumn();
							        if (column >= 0) {
								        String columnName = editableModel.getColumnName(column);
								        Object data = editableModel.getValueAt(row, column);
								        
										FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
										FeatureId featureId = ff.featureId((String)editableModel.getValueAt(row, editableModel.getIdCol()));
										Filter filter = ff.id(featureId);
								        
								        Transaction t1 = new DefaultTransaction("transaction 1");
								        store.setTransaction(t1);
								        
								        try {
											store.modifyFeatures(columnName, data, filter);
											t1.commit();
									        t1.close();
											System.out.println(featureId + "-" + columnName + "数据已更改。");
										} catch (IOException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
							        }
								}
							}
				        });
				        
						table.setModel(editableModel);
					}
		        	
		        });
		        JMenuItem stopEditItem = new JMenuItem(" 停止编辑 ");
		        stopEditItem.addActionListener(new ActionListener() {
		        	@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						table.setModel(uneditableModel);
					}
		        });
		        attributeMenu.add(startEditItem);
		        attributeMenu.add(stopEditItem);
		        JMenuBar attributeMenuBar = new JMenuBar();
		        attributeMenuBar.add(attributeMenu);
		        
		        JDialog attributeDialog = new JDialog(mainFrame, "属性表", false);
		        attributeDialog.setSize(1000, 500);
		        attributeDialog.setLocationRelativeTo(null);
		        attributeDialog.setJMenuBar(attributeMenuBar);
		        attributeDialog.add(scrollPane);
				
		        attributeDialog.setVisible(true);
				
			} catch (IOException | CQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
		}else {
			// 没有选中的图层
			JOptionPane.showMessageDialog(mainFrame, "请先选中要进行属性编辑的图层");
			System.out.println("未找到已选中图层。");
		}

	}
	
	
	// 移除鼠标事件监听函数
	protected void removeMML() {
		if (mML != null) {
			mainFrame.getMapPane().removeMouseListener(mML);
		}
	}
	
	protected Layer getLayerFromShp(File file) {
		Layer shpLayer = null;
		if (file == null) {
			shpLayer = null;
		}

		FileDataStore store;
		SimpleFeatureSource featureSource;
		try {
			store = FileDataStoreFinder.getDataStore(file);
			featureSource = store.getFeatureSource();
			
			StyleCreator sc = new StyleCreator();
			//Style shpStyle = SLD.createSimpleStyle(featureSource.getSchema());
			Style shpStyle = sc.createShpStyle2(featureSource);
			shpLayer = new FeatureLayer(featureSource, shpStyle);
			
			// 设置图层title
			String title = file.getName().toLowerCase();
			if (title.endsWith(".shp")) {
				title = title.substring(0, title.length() - 4);
			}
			shpLayer.setTitle(title);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return shpLayer;
	}

	protected Layer getLayerFromRaster(File file) {
		Layer rasterLayer = null;
		if (file == null) {
			rasterLayer = null;
		}
		
		AbstractGridFormat format = GridFormatFinder.findFormat(file); 
        //this is a bit hacky but does make more geotiffs work
        Hints hints = new Hints();
        if (format instanceof GeoTiffFormat) {
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        }
        
        GridCoverage2DReader reader;
        reader = format.getReader(file, hints);
        
        StyleCreator sc = new StyleCreator();
        Style rasterStyle = sc.createRGBStyle(reader);
        rasterLayer = new GridReaderLayer(reader, rasterStyle);
        
        // 设置图层title
		String title = file.getName().toLowerCase();
		if (title.endsWith(".jpg") || title.endsWith(".png") || title.endsWith(".gif")) {
			title = title.substring(0, title.length() - 4);
		} else if (title.endsWith(".jpeg")) {
			title = title.substring(0, title.length() - 5);
		}
		rasterLayer.setTitle(title);
        return rasterLayer;
	}

	
	

    private static SimpleFeatureType createFeatureType(CoordinateReferenceSystem  mapCRS, int typeFlag) {

    	/**
    	 * @param mapCRS the Coordinate Reference System of the current map
    	 * @param typeFlag the flag of feature type, 0 to create a Point feature Type, 1 for Line feature type, 2 for Polygon feature type
    	 * 
    	 */
    	SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    	switch (typeFlag) {
    		
    		case 0:  // 点要素类型
    			builder.setName("Point");
		        builder.setCRS(mapCRS); // <- Coordinate reference system
		        builder.add("the_geom", Point.class);
		        final SimpleFeatureType Point = builder.buildFeatureType();
		        return Point;
    		case 1:   // 线要素类型
    			builder.setName("Line");
		        builder.setCRS(mapCRS); 
		        builder.add("the_geom", LineString.class);
		        final SimpleFeatureType Line = builder.buildFeatureType();
		        return Line;
    		case 2:  // 多边形要素类型
    			builder.setName("Poly");
		        builder.setCRS(mapCRS); 
		        builder.add("the_geom", Polygon.class);
		        final SimpleFeatureType Poly = builder.buildFeatureType();
		        return Poly;
    		default:
    			System.out.println("创建要素类型失败，请检查参数。");
    			return null;
    	}
        
    }


}
