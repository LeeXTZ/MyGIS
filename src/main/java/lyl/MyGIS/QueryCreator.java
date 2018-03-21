
package lyl.MyGIS;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.map.Layer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JDataStoreWizard;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.geotools.swing.wizard.JWizard;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 定义QueryCreator类，封装空间查询和属性查询。
 * 
 * attributeQuery()方法，新建一个属性查询对话框，用户可以选择不同的数据源（shp文件、PostGIS空间数据库、DataStore），
 * 可以选择获取符合条件的要素、计数、获取坐标、获取要素的平均中心点。
 * 
 * filterFeatures()方法，获取用户在输入框中输入的条件，将其转换成Filter对象，
 * 然后利用source.getFeatures(filter)方法获取符合条件的要素集，并将要素集显示在表格中；
 * 
 * countFeatures()方法，获取用户在输入框中输入的条件，将其转换成Filter对象，
 * 然后利用source.getFeatures(filter)方法获取符合条件的要素集，最后弹框显示符合条件的要素的数量；
 * 
 * queryFeatures()方法，利用Query对象查询符合条件的要素级的几何信息，并显示在表格中；
 * 
 * centerFeatures()方法，首先获取符合条件的要素集，然后计算要素集的平均中心，并显示。
 * 
 * @author 114151班-20151001251-李彦磊
 * 
 */
@SuppressWarnings("serial")
public class QueryCreator {
    private DataStore dataStore;
    private JComboBox<String> layerCombo;
    private JTable table;
    private JTextField text;

    public void attributeQuery(JMapFrame mainFrame) {
    	JDialog queryDialog = new JDialog(mainFrame, "属性查询", false);
    	
    	Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int width = screenSize.width;
		int height = screenSize.height;
    	queryDialog.setSize(width/3, height*7/18);
    	queryDialog.setLocationRelativeTo(mainFrame);

    	//queryFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	queryDialog.getContentPane().setLayout(new BorderLayout());

        text = new JTextField(80);
        text.setText("include"); // include selects everything!
        queryDialog.getContentPane().add(text, BorderLayout.NORTH);

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setModel(new DefaultTableModel(5, 5));
        table.setPreferredScrollableViewportSize(new Dimension(500, 200));

        JScrollPane scrollPane = new JScrollPane(table);
        queryDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);

        JMenuBar menubar = new JMenuBar();
        queryDialog.setJMenuBar(menubar);

        layerCombo = new JComboBox<>();
        menubar.add(layerCombo);

        JMenu fileMenu = new JMenu(" File ");
        menubar.add(fileMenu);
        
        JMenu dataMenu = new JMenu(" 【Query】 ");
        menubar.add(dataMenu);
        
        queryDialog.pack();

        // start file menu
        fileMenu.add(new SafeAction("Open shapefile...") {
            public void action(ActionEvent e) throws Throwable {
                connect(new ShapefileDataStoreFactory());
            }
        });
        fileMenu.add(new SafeAction("Connect to PostGIS database...") {
            public void action(ActionEvent e) throws Throwable {
                connect(new PostgisNGDataStoreFactory());
            }
        });
        fileMenu.add(new SafeAction("Connect to DataStore...") {
            public void action(ActionEvent e) throws Throwable {
                connect(null);
            }
        });
        fileMenu.addSeparator();
        fileMenu.add(new SafeAction("Exit") {
            public void action(ActionEvent e) throws Throwable {
                System.exit(0);
            }
        });
        // end file menu

        // start data menu
        dataMenu.add(new SafeAction("Get features") {
            public void action(ActionEvent e) throws Throwable {
                filterFeatures();
            }
        });
        dataMenu.add(new SafeAction("Count") {
            public void action(ActionEvent e) throws Throwable {
                countFeatures();
            }
        });
        dataMenu.add(new SafeAction("Geometry") {
            public void action(ActionEvent e) throws Throwable {
                queryFeatures();
            }
        });
        // end data menu
        dataMenu.add(new SafeAction("Center") {
            public void action(ActionEvent e) throws Throwable {
                centerFeatures();
            }
        });
        queryDialog.setVisible(true);
    }
    
    public SimpleFeatureCollection spatialQuery(Layer layer, DirectPosition2D nWPos, DirectPosition2D sEPos) throws CQLException, IOException {
    	
        SimpleFeatureSource source = (SimpleFeatureSource) layer.getFeatureSource();

		Filter filter = CQL
				.toFilter("BBOX(the_geom, " + nWPos.x + "," + nWPos.y + "," + sEPos.x + "," + sEPos.y + ")");
        SimpleFeatureCollection features = source.getFeatures(filter);
        
        return features;
    }
    
    // start connect
    private void connect(DataStoreFactorySpi format) throws Exception {
        JDataStoreWizard wizard = new JDataStoreWizard(format);
        int result = wizard.showModalDialog();
        if (result == JWizard.FINISH) {
            Map<String, Object> connectionParameters = wizard.getConnectionParameters();
            dataStore = DataStoreFinder.getDataStore(connectionParameters);
            if (dataStore == null) {
                JOptionPane.showMessageDialog(null, "Could not connect - check parameters");
            }
            updateUI();
        }
    }
    // end connect

    // start update
    private void updateUI() throws Exception {
        ComboBoxModel<String> cbm = new DefaultComboBoxModel<>(dataStore.getTypeNames());
        layerCombo.setModel(cbm);

        table.setModel(new DefaultTableModel(5, 5));
    }
    // end update

    // start filterFeatures
    private void filterFeatures() throws Exception {
        String typeName = (String) layerCombo.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
    // end filterFeatures

    // start countFeatures
    private void countFeatures() throws Exception {
        String typeName = (String) layerCombo.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);

        int count = features.size();
        JOptionPane.showMessageDialog(text, "Number of selected features:" + count);
    }
    // end countFeatures

    // start queryFeatures
    private void queryFeatures() throws Exception {
        String typeName = (String) layerCombo.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        FeatureType schema = source.getSchema();
        String name = schema.getGeometryDescriptor().getLocalName();

        Filter filter = CQL.toFilter(text.getText());

        Query query = new Query(typeName, filter, new String[] { name });

        SimpleFeatureCollection features = source.getFeatures(query);

        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
    // end queryFeatures

    // start centerFeatures
    private void centerFeatures() throws Exception {
        String typeName = (String) layerCombo.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());

        FeatureType schema = source.getSchema();
        String name = schema.getGeometryDescriptor().getLocalName();
        Query query = new Query(typeName, filter, new String[] { name });

        SimpleFeatureCollection features = source.getFeatures(query);

        double totalX = 0.0;
        double totalY = 0.0;
        long count = 0;
        try (SimpleFeatureIterator iterator = features.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Point centroid = geom.getCentroid();
                totalX += centroid.getX();
                totalY += centroid.getY();
                count++;
            }
        }
        double averageX = totalX / (double) count;
        double averageY = totalY / (double) count;
        Coordinate center = new Coordinate(averageX, averageY);

        JOptionPane.showMessageDialog(text, "Center of selected features:" + center);
    }
    // end centerFeatures

}

