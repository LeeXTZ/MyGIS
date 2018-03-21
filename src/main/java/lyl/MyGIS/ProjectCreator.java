/**
 * 
 */
package lyl.MyGIS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.geotools.map.Layer;
import org.geotools.map.MapContent;

/**
 * 定义一个类，用于描述软件使用中的 “项目” 这一概念 <br/>
 * <br/>
 * （1）字段prjName，用于存储项目名称 ； 
 * （2）静态字段prjName，用于标记项目序号 ； 
 * （3）字段layerPathList，用于存储项目项目中的图层的规范化绝对路径 ；
 * （4）字段prjFile，用于存储项目对应的项目文件，每个项目文件中会存储对应项目的信息。 <br/>
 * <br/>
 * 重载构造函数，一个是无参数构造函数，用于新建一个空 “项目”； 另一个参数为File对象，用于读取给定的项目文件并构造一个 “项目”对象。 <br/>
 * <br/>
 * newPrjFile(File file)方法，以一个文件对象作为参数，
 * 		向此文件对象中保存项目的相关信息（其中，项目名由文件对象的getName()方法获得 ）。
 * 		可用于保存从未保存过的新项目或另存为新项目。
 * 		在【新建项目】菜单项的事件处理程序中调用这个方法，以新建文件对话框获得的文件对象作为参数。
 * <br/>
 * savePrjFile()方法，用于将项目信息保存到Project对象的prjFile字段中。
 * 		由于此方法需要用到prjFile字段，这就要求要保存的项目必须已经保存过（存在项目文件）。
 * 
 * @author 114151班-20151001251-李彦磊
 *
 */
public class ProjectCreator {

	private String prjName;
	private static int prjNumber = 0;
	private ArrayList<String> layerPathList; // 定义一个存放图层路径的数组列表
	private File prjFile;
	//private boolean isSaved;

	// 无参数构造函数
	public ProjectCreator() {
		++ProjectCreator.prjNumber;
		this.setPrjName("NewProject" + prjNumber);
		this.setLayerPathList(new ArrayList<String>());
		//this.isSaved = false;

	}

	// 读取项目文件进行构造的构造函数
	public ProjectCreator(File prjFile) {
		Scanner reader = null;
		try {
			reader = new Scanner(prjFile);

			ArrayList<String> pathList = new ArrayList<String>();
			//String currLine = reader.nextLine();
			while (reader.hasNextLine()) {
				String currLine = reader.nextLine();
				if(currLine.startsWith("@prjName@")) {
					this.setPrjName(currLine.substring(9));
				}else if(currLine.startsWith("@layer@")) {
					pathList.add(currLine.substring(7));
				}
				
			}
			this.setLayerPathList(pathList);

			this.prjFile = prjFile;
			//this.isSaved = true;

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

	}
	
	public int getPrjNumber() {
		return ProjectCreator.prjNumber;
	}

	public String getPrjName() {
		return prjName;
	}

	public void setPrjName(String prjName) {
		this.prjName = prjName;
	}

	public ArrayList<String> getLayerPathList() {
		return layerPathList;
	}

	public void setLayerPathList(ArrayList<String> layerPathList) {
		this.layerPathList = layerPathList;
	}

	public void newPrjFile(File file) {
		/**
		 * 
		 * 新建项目文件并保存
		 */
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(file);
			writer.println("**** MyGIS 0.0.1-SNAPSHOT ****");
			// *********获取用户对话框输入作为项目名**********
			String prjNameWithoutExtension = null;
			if(file.getName().endsWith(".mgprj")) {
				prjNameWithoutExtension = file.getName().substring(0, file.getName().length() - 6);
			}else {
				prjNameWithoutExtension = file.getName();
			}
			
			writer.println("@prjName@" + prjNameWithoutExtension);
			// writer.println("@isSaved@" + this.isSaved);
			if (this.getLayerPathList() != null) {
				Iterator<String> iter = this.getLayerPathList().iterator();
				while (iter.hasNext()) {
					String layerPath = iter.next();
					writer.println("@layer@" + layerPath);
				}
			}
			this.prjFile = file;
			//this.isSaved = true;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

	public void savePrjFile() {
		/**
		 * 向已存在的项目文件中保存更改。
		 */

		// 向文件写入
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(this.prjFile);
			writer.println("**** MyGIS 0.0.1-SNAPSHOT *****");

			String prjNameWithoutExtension = null;
			if(this.prjFile.getName().endsWith(".mgprj")) {
				prjNameWithoutExtension = this.prjFile.getName().substring(0, this.prjFile.getName().length() - 6);
			}else {
				prjNameWithoutExtension = this.prjFile.getName();
			}
			
			writer.println("@prjName@" + prjNameWithoutExtension);
			Iterator<String> iter = this.getLayerPathList().iterator();
			while (iter.hasNext()) {
				String layerPath = iter.next();
				writer.println("@layer@" + layerPath);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

}
