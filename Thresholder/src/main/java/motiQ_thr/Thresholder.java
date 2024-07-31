/***===============================================================================
 *  
 * MotiQ_thresholder plugin for ImageJ
 * 
 * Copyright (C) 2015-2024 Jan N. Hansen
 * First version: January 05, 2015
 * This version: July 31, 2024
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * 
 * For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
 * 
 * =============================================================================**/

package motiQ_thr;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.UIManager;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.process.*;
import ij.plugin.*;
import ij.plugin.frame.Recorder;
import java.text.*;

public class Thresholder implements PlugIn, Measurements{//, DialogListener {
	//Name variables
	final static String PLUGINNAME = "MotiQ Thresholder";
	final static String PLUGINVERSION = "v0.2.4";
	
	//Fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	
	//Formats
	static final SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
	static final DecimalFormat dformat0 = new DecimalFormat("#0",new DecimalFormatSymbols(Locale.US));
	static final DecimalFormat dformat1 = new DecimalFormat("#0.000000",new DecimalFormatSymbols(Locale.US));
	static final DecimalFormat dformat2 = new DecimalFormat("#0.00",new DecimalFormatSymbols(Locale.US));
	DecimalFormat dformatdialog = new DecimalFormat("#0.000000");
	
//	final static String[] DSLItems = {"3D Analysis (CLSM, 2PM)", "2D in vivo / acute slices (live 2PM)", "2D in vitro (epiFM)"};
	
	//-----------------define numbers for Dialog-----------------
	static final String[] taskVariant = {"the active, open image",
			"multiple images (use multi-task manager to open images)",
			"all open images"};
	String selectedTaskVariant = taskVariant[1];
	int tasks = 1;
	
	boolean useAlternateRef = true;
	boolean getImageByName = true;
	String nameSuffix = "_CUT";
	String parentEnding = ".tif";
	boolean restrictToPos = true;
	
	double scalingFactor = 0.5;
	
	boolean onlyTimeGroup = false;
	int startGroup = 1;
	int endGroup = 10;
	
	boolean separateFrames = true;
	
	boolean conv8Bit = true,
			keepIntensities = true,
			fillHoles = false,
			localThreshold = false,
			autoSaveImage = true,
			saveDate = false;
	
	int locThrRadius = 50;
	final static String[] stackMethod = {"apply average threshold of independent stack-image thresholds",
			"apply threshold determined in the stack histogram",
			"threshold every stack image independently",
			"apply threshold determined in a maximum-intensity-projection",
			"no stack processing"};	
	//TODO only of selected Z / no Z
	String chosenStackMethod = stackMethod[3];
				
	String[] algorithm = {"Default", "IJ_IsoData", "Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"};
	String selectedAlgorithm = "MinError";
	
	//Multi-task management
	ProgressDialog progress;
	boolean processingDone = false;	
	boolean continueProcessing = true;

	boolean record = false;
	boolean noGUIs = false;
	
public void run(String arg) {
	dformatdialog.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));

	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	//-----------Eventually read settings from macro input------------------------
	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	boolean showDialog = true;

if (IJ.macroRunning() 
		&& Macro.getOptions() != null
		&& Macro.getOptions().length()>0) {
    	String macroOptions = Macro.getOptions();    	
//    	IJ.log("Macro Options: " + macroOptions);
    	
    	String temp;	
    	selectedTaskVariant = taskVariant[0];

		temp = "";
    	if(macroOptions.contains("use-alternate")) {
    		useAlternateRef = true;
    		
    		if(macroOptions.contains("automatically-find")) {
    			getImageByName = true;
    		}else {
    			getImageByName = false;
    		}
//			IJ.log("detected getImageByName: " + getImageByName);
    		
    		temp = "";
    		if(macroOptions.contains("begin-of-suffix=")){
    			temp = macroOptions.substring(macroOptions.indexOf("begin-of-suffix="));
    			temp = temp.substring(temp.indexOf("=")+1);
    			if(temp.startsWith("[") || temp.startsWith("//[")) {
        			temp = temp.substring(1,temp.indexOf("]"));    				
    			}else {
        			temp = temp.substring(0,temp.indexOf(" "));
    			}    			
        		nameSuffix = temp;
//    			IJ.log("detected suffixbegin: " + nameSuffix);
    		}

    		if(macroOptions.contains("additional-suffix=")){
    			temp = macroOptions.substring(macroOptions.indexOf("additional-suffix="));
    			temp = temp.substring(temp.indexOf("=")+1);
    			if(temp.startsWith("[") || temp.startsWith("//[")) {
        			temp = temp.substring(1,temp.indexOf("]"));    				
    			}else {
        			temp = temp.substring(0,temp.indexOf(" "));
    			}    			
        		parentEnding = temp;
//    			IJ.log("detected parentEnding: " + parentEnding);
    		}
    		
    		if(macroOptions.contains("restrict")) {
    			restrictToPos = true;
    		}else {
    			restrictToPos = false;    			
    		}
//			IJ.log("detected restrictToPos: " + restrictToPos);
    	}else {
    		useAlternateRef = false;
   			getImageByName = false;
   			restrictToPos = false;
    	}
    	
    	if(macroOptions.contains("scale=")){
			temp = macroOptions.substring(macroOptions.indexOf("scale="));
    		temp = temp.substring(temp.indexOf("=")+1,temp.indexOf(" "));
    		scalingFactor = Double.parseDouble(temp);
//    		IJ.log("detected scalingFactor: " + scalingFactor);
		}
    	
    	if(macroOptions.contains("convert")) {
			conv8Bit = true;
		}else {
			conv8Bit = false;
		}
//		IJ.log("detected conv8Bit: " + conv8Bit);
    	    	
    	if(macroOptions.contains("threshold=")){
			temp = macroOptions.substring(macroOptions.indexOf("threshold="));
    		temp = temp.substring(temp.indexOf("=")+1,temp.indexOf(" "));
    		selectedAlgorithm = temp;
    		
    		boolean thresholdFits = false;
    		for(int i = 0; i < algorithm.length; i++) {
    			if(selectedAlgorithm.equals(algorithm[i])) {
    				thresholdFits = true;
    				break;
    			}
    		}
    		if(thresholdFits == false) {
    			IJ.error("Unknown threshold algorithm specified!\nSelect a threshold algorithm available in MotiQ Thresholder!");
    		}
    		
//    		IJ.log("detected selectedAlgorithm: " + selectedAlgorithm);    		
		}
    	
    	if(macroOptions.contains("stack-handling=")){
			temp = macroOptions.substring(macroOptions.indexOf("stack-handling="));
    		temp = temp.substring(temp.indexOf("=")+1);
    		   		
    		for(int i = 0; i < algorithm.length; i++) {
    			if(temp.contains(stackMethod[i])) {
    				chosenStackMethod = stackMethod[i];
    				break;
    			}
    			if(i == algorithm.length-1) {
    				IJ.error("Stack handling method was not correctly specified!");
    			}
    		}    		
//    		IJ.log("detected chosenStackMethod: " + chosenStackMethod);
		}

    	if(macroOptions.contains("threshold-every-time-step")) {
			separateFrames = true;
		}else {
			separateFrames = false;
		}
//		IJ.log("detected separateFrames: " + separateFrames);
    	
    	if(macroOptions.contains("threshold-only-distinct-times")) {
			onlyTimeGroup = true;
			
			if(macroOptions.contains("start-time=")){
				temp = macroOptions.substring(macroOptions.indexOf("start-time="));
	    		temp = temp.substring(temp.indexOf("=")+1,temp.indexOf(" "));
	    		startGroup = Integer.parseInt(temp);
//	    		IJ.log("detected startGroup: " + startGroup);
			}
			
			if(macroOptions.contains("end-time=")){
				temp = macroOptions.substring(macroOptions.indexOf("end-time="));
	    		temp = temp.substring(temp.indexOf("=")+1,temp.indexOf(" "));
	    		endGroup = Integer.parseInt(temp);
//	    		IJ.log("detected endGroup: " + endGroup);
			}
		}else {
			onlyTimeGroup = false;
		}
//		IJ.log("detected onlyTimeGroup: " + onlyTimeGroup);
    	
    	if(macroOptions.contains("local-threshold")) {
			localThreshold = true;
//			IJ.log("detected localThreshold: " + localThreshold);
			
			if(macroOptions.contains("local-threshold-radius=")){
				temp = macroOptions.substring(macroOptions.indexOf("local-threshold-radius="));
	    		temp = temp.substring(temp.indexOf("=")+1,temp.indexOf(" "));
	    		locThrRadius = Integer.parseInt(temp);
//	    		IJ.log("detected locThrRadius: " + locThrRadius);
			}
		}else {
			localThreshold = false;
		}
//		IJ.log("detected localThreshold: " + localThreshold);
    	
    	if(macroOptions.contains("fill-holes")) {
			fillHoles = true;
    	}else {
    		fillHoles = false;
    	}
//		IJ.log("detected fillHoles: " + fillHoles);

    	if(macroOptions.contains("keep-intensities")) {
    		keepIntensities = true;
    	}else {
    		keepIntensities = false;
    	}
//		IJ.log("detected keepIntensities: " + keepIntensities);

    	if(macroOptions.contains("automatically-save")) {
    		autoSaveImage = true;
    	}else {
    		autoSaveImage = false;    		
    	}
//		IJ.log("detected autoSaveImage: " + autoSaveImage);

		if(macroOptions.contains("include-date")){
			saveDate = true;
		}else {
			saveDate = false;
		}
//		IJ.log("detected saveDate: " + saveDate);

		if(macroOptions.contains("nogui") || macroOptions.contains("noGUI") || macroOptions.contains("NOGUI")){
    		noGUIs = true;
//			IJ.log("detected noGUIs: " + noGUIs);
		}else {
			noGUIs = false;
		}
		
        showDialog = false;
    }    

    record = false;

    if(showDialog && !noGUIs) {
    	if(Recorder.record) {
    		record = true;
    		Recorder.record = false;
    	}
		/**&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& *
		 *									display dialog
		 * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& */
			
		GenericDialog gd = new GenericDialog(PLUGINNAME + " - Settings");
    	gd.addHelp("https://github.com/hansenjn/MotiQ/wiki");
		//	gd.setInsets(0,0,0); (top, left, bottom)
		
		gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", version " + PLUGINVERSION 
				+ " (\u00a9 2015 - " + yearOnly.format(new Date()) + ", Jan Niklas Hansen)", SuperHeadingFont);
		
		gd.setInsets(5,0,0);	gd.addChoice("Process ", taskVariant, selectedTaskVariant);
		
		gd.setInsets(5,0,0);	gd.addMessage("Reference image", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addCheckbox("Use alternate reference image",useAlternateRef);
		gd.setInsets(0,20,0);	gd.addCheckbox("-> Automatically find alternate reference image in origin directory (via file name)",getImageByName);
		gd.setInsets(0,50,0);	gd.addStringField("-> Begin of suffix in file name of input image",nameSuffix);
		gd.setInsets(0,50,0);	gd.addStringField("-> Additional suffix in file name of alternate reference image",parentEnding);
		gd.setInsets(0,20,0);	gd.addCheckbox("-> Restrict threshold calculation to size and position of input image",restrictToPos);
		
		gd.setInsets(5,0,0);	gd.addMessage("Pre-processing", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addNumericField("Scale down reference image for calculation - factor: ", scalingFactor, 1);
		gd.setInsets(0,0,0);	gd.addCheckbox("Convert input and reference image into 8-bit before processing.",conv8Bit);	//TODO save into metadata
			
		gd.setInsets(5,0,0);	gd.addMessage("Threshold determination", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addChoice("Threshold algorithm", algorithm, selectedAlgorithm);
		gd.setInsets(10,0,0);	gd.addChoice("Stack handling: ", stackMethod, chosenStackMethod);	
		gd.setInsets(5,0,0);	gd.addCheckbox("Threshold every time-step separately.", separateFrames);
		gd.setInsets(0,0,0);	gd.addCheckbox("Threshold only distinct time series. Start / end time-step: ",onlyTimeGroup);
		gd.setInsets(-23,0,0);	gd.addNumericField("", startGroup, 0);
		gd.setInsets(-23,55,0);	gd.addNumericField("", endGroup, 0);	
		gd.setInsets(0,0,0);	gd.addCheckbox("Calculate an individual threshold for each pixel - local threshold radius [px]: ",localThreshold);
		gd.setInsets(-21,45,0);	gd.addNumericField("", locThrRadius, 0);		
			
		gd.setInsets(5,0,0);	gd.addMessage("Image segmentation and output", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addCheckbox("Fill holes in mask (independent for every stack image)",fillHoles);
		gd.setInsets(0,0,0);	gd.addCheckbox("Keep original intensities in pixels above threshold",keepIntensities);
		gd.setInsets(0,0,0);	gd.addCheckbox("Automatically save image/metadata into origin directory and close image", autoSaveImage);
		gd.setInsets(0,0,0);	gd.addCheckbox("Include date in output file names", saveDate);
		
		gd.showDialog();
		
		//get variables
		selectedTaskVariant = gd.getNextChoice();
		
		useAlternateRef = gd.getNextBoolean();
		getImageByName = gd.getNextBoolean();
		nameSuffix = gd.getNextString();
		parentEnding = gd.getNextString();
		restrictToPos = gd.getNextBoolean();
		
		if(useAlternateRef == false){
			getImageByName = false;
			restrictToPos = false;
		}
		
		scalingFactor = (double)gd.getNextNumber();
		conv8Bit = gd.getNextBoolean();	
		
		selectedAlgorithm = gd.getNextChoice();
		chosenStackMethod = gd.getNextChoice();
		separateFrames = gd.getNextBoolean();
		onlyTimeGroup = gd.getNextBoolean();
		startGroup = (int)gd.getNextNumber();
		endGroup = (int)gd.getNextNumber();	
					
		localThreshold = gd.getNextBoolean();
		locThrRadius = (int)gd.getNextNumber();
		
		fillHoles = gd.getNextBoolean();
		keepIntensities = gd.getNextBoolean();
		autoSaveImage = gd.getNextBoolean();
		saveDate = gd.getNextBoolean();
		
		if (gd.wasCanceled()) return;
		
    	if(record) {
    		Recorder.record = true;
    	}
    }
    
    //Create macro recording string if macro recording activated:
    if (record) {  
    	String recordString = "";
    	if(useAlternateRef) {
    		recordString += "use-alternate ";
    		
    		if(getImageByName) {
        		recordString += "automatically-find ";
    		}
    		
    		recordString += "begin-of-suffix=[" + nameSuffix + "] ";
    		
    		recordString += "additional-suffix=[" + parentEnding + "] ";
    		
    		if(restrictToPos) {
        		recordString += "restrict ";
    		}
    	}

    	recordString += "scale=" + dformatdialog.format(scalingFactor) + " ";
    	
    	if(conv8Bit) {
    		recordString += "convert ";
		} 

		recordString += "threshold=" + selectedAlgorithm + " ";
    	
		recordString += "stack-handling=[" + chosenStackMethod + "] ";
    	
		if(separateFrames) {
    		recordString += "threshold-every-time-step ";
		}
		
		if(onlyTimeGroup) {
    		recordString += "threshold-only-distinct-times ";
        	recordString += "start-time=" + startGroup + " ";
        	recordString += "end-time=" + endGroup + " ";
		}

		if(localThreshold) {
    		recordString += "local-threshold ";
        	recordString += "local-threshold-radius=" + locThrRadius + " ";
		}

		if(fillHoles) {
    		recordString += "fill-holes ";
		}

		if(keepIntensities) {
    		recordString += "keep-intensities ";
		}
		
		if(autoSaveImage) {
    		recordString += "automatically-save ";
		}
    	
    	if(saveDate) {
        	recordString += "include-date ";    		
    	}
    	
    	recordString = recordString.substring(0,recordString.length()-1);

		Recorder.record = true;
		
    	Recorder.recordString("run(\"" + PLUGINNAME + " (" + PLUGINVERSION + ")\",\"" + recordString + "\");\n");
    }
	
/**&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
 *							load image tasks
 **&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	
	String parName  = "";
	String parDir = "";
		
	if(useAlternateRef==true&&getImageByName==false){
		//get folder and file name of parent---------------------------------
			new WaitForUserDialog("Press OK! -> a dialog will pop up: use it to select the parent image for images to process!").show();
			OpenDialog od = new OpenDialog("Open Parent Image", null);
			parName  = od.getFileName();
			parDir = od.getDirectory();
			if(selectedTaskVariant.equals(taskVariant[1])){
				new WaitForUserDialog("Press OK to continue -> load all images to process into the Multi Task Manager!").show();
			}			
		//get folder and file name of parent---------------------------------
	}
	
	//Improved file selector
		try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
			
		String name [] = {"",""};
		String dir [] = {"",""};
		ImagePlus allImps [] = new ImagePlus [2];		
		if(selectedTaskVariant.equals(taskVariant[1])){
			OpenFilesDialog od = new OpenFilesDialog ();
			od.setLocation(0,0);
			od.setVisible(true);
			
			od.addWindowListener(new java.awt.event.WindowAdapter() {
		        public void windowClosing(WindowEvent winEvt) {
//		        	IJ.log("Analysis canceled!");
		        	return;
		        }
		    });
		
			//Waiting for od to be done
			while(od.done==false){
				try{
					Thread.currentThread().sleep(50);
			    }catch(Exception e){
			    }
			}
			
			tasks = od.filesToOpen.size();
			name = new String [tasks];
			dir = new String [tasks];
			for(int task = 0; task < tasks; task++){
				name[task] = od.filesToOpen.get(task).getName();
				dir[task] = od.filesToOpen.get(task).getParent() + System.getProperty("file.separator");
			}		
		}else if(selectedTaskVariant.equals(taskVariant[0])){
			if(WindowManager.getIDList()==null){
				IJ.error("Plugin canceled - no image open!");
				return;
			}
			FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
			try {
				name [0] = info.fileName;	//get name
				dir [0] = info.directory;	//get directory
			}catch(Exception e) {
				IJ.error(PLUGINNAME + " cannot retrieve where the image " + WindowManager.getCurrentImage().getTitle() + " is saved. \nSave the image and relaunch " + PLUGINNAME + ".");
				return;
			}
			if(!new File(dir[0] + name[0]).exists()){
				IJ.error(PLUGINNAME + " cannot retrieve where the image " + WindowManager.getCurrentImage().getTitle() + " is saved. \nSave the image and relaunch " + PLUGINNAME + ".");
				return;
			}
			tasks = 1;
		}else if(selectedTaskVariant.equals(taskVariant[2])){	// all open images
			if(WindowManager.getIDList()==null){
				IJ.error("Plugin canceled - no image open!");
				return;
			}
			int IDlist [] = WindowManager.getIDList();
			tasks = IDlist.length;	
			if(tasks == 1){
				selectedTaskVariant=taskVariant[0];
				FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
				try {
					name [0] = info.fileName;	//get name
					dir [0] = info.directory;	//get directory
				}catch(Exception e) {
					IJ.error(PLUGINNAME + " cannot retrieve where the image " + WindowManager.getCurrentImage().getTitle() + " is saved. \nSave the image and relaunch " + PLUGINNAME + ".");
					return;
				}
				if(!new File(dir[0] + name[0]).exists()){
					IJ.error(PLUGINNAME + " cannot retrieve where the image " + WindowManager.getCurrentImage().getTitle() + " is saved. \nSave the image and relaunch " + PLUGINNAME + ".");
					return;
				}
			}else{
				name = new String [tasks];
				dir = new String [tasks];
				allImps = new ImagePlus [tasks];
				for(int i = 0; i < tasks; i++){
					allImps[i] = WindowManager.getImage(IDlist[i]); 
					FileInfo info = allImps[i].getOriginalFileInfo();
					try {
						name [i] = info.fileName;	//get name
						dir [i] = info.directory;	//get directory
					}catch(Exception e) {
						IJ.error(PLUGINNAME + " cannot retrieve where the image " + allImps[i].getTitle() + " is saved. \nSave the image and relaunch " + PLUGINNAME + ".");
						return;
					}
					if(!new File(dir[i] + name[i]).exists()){
						IJ.error(PLUGINNAME + " cannot retrieve where the image " + allImps[i].getTitle() + " is saved. \nSave the image and relaunch " + PLUGINNAME + ".");
						return;
					}
				}		
			}
					
		}
		
		//add progressDialog
		if(!noGUIs) {
			progress = new ProgressDialog(name, tasks);
//			progress.setLocation(0,0);
			progress.setVisible(true);
//			progress.setAlwaysOnTop(true);
//			progress.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
			progress.addWindowListener(new java.awt.event.WindowAdapter() {
		        public void windowClosing(WindowEvent winEvt) {
		        	if (record) {	
		        		Recorder.record = true;
		        	}
		        	if(processingDone==false){
		        		IJ.error("Script stopped...");
		        	}
		        	continueProcessing = false;
		        	return;
		        }
		    });
		}
		 	
/**&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
 *							Prepare images
 **&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	for(int task = 0; task < tasks; task++){
		if(!noGUIs) progress.updateBarText("in progress...");
		Date startDate = new Date();
		running: while(continueProcessing){
			int width = 0, height = 0, stacksize = 0, frames = 0, slices = 0;
			
			//Check for problems with image file
				if(name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".txt")){
					if(!noGUIs) {
						progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": File is no image! Could not be processed!", ProgressDialog.ERROR);
						progress.moveTask(task);
					}	
					break running;
				}
				if(name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".zip")){
					if(!noGUIs) {
						progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": File is no image! Could not be processed!", ProgressDialog.ERROR);
						progress.moveTask(task);
					}	
					break running;
				}
			//Check for problems with image file
				
			//open parent image by input image name
			if(getImageByName){
				String parentname = name[task];
				if(parentname.contains(nameSuffix)){
					parentname = name[task].substring(0,name[task].lastIndexOf(nameSuffix));	//Remove Datatype-Ending from name
					parentname += parentEnding;
	//				IJ.log("Task " + (task+1) + "/" + tasks + ": Parentname: " + parentname);
					parName = parentname;
					parDir = dir [task];
				}else{
					if(!noGUIs) {
						progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": no parent image name could be determined - suffix could not be found! Image cannot be processed since parent image cannot be determined!", ProgressDialog.ERROR);
						progress.moveTask(task);
					}
					break running;
				}			
			}
				
			
			//open Image
			   	ImagePlus imp;
			   	try{
			   		if(selectedTaskVariant.equals(taskVariant[1])){
			   			imp = IJ.openImage(""+dir[task]+name[task]+"");
			   		}else if(selectedTaskVariant.equals(taskVariant[0])){
			   			imp = WindowManager.getCurrentImage();
			   		}else{
			   			imp = allImps[task];
			   		}			   			
					imp.deleteRoi();
			   	}catch (Exception e) {
			   		if(!noGUIs) {
			   			progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": file is no image - could not be processed!", ProgressDialog.ERROR);
						progress.moveTask(task);	
			   		}					
			   		break running;
				}
			   	
			   	if(imp.getNChannels() != 1){
			   		if(!noGUIs) {
				   		progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": cannot process multi-channel images!", ProgressDialog.ERROR);
						progress.moveTask(task);			   			
			   		}	
					break running;
			   	}
			   				   	
				width = imp.getWidth();
				height = imp.getHeight();
				stacksize = imp.getStackSize();
				if(stacksize == 1 && chosenStackMethod.equals(stackMethod[4])==false){
					//if image is no stack, process as non-stack
					chosenStackMethod = stackMethod[4];
					if(!noGUIs) progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": image is no stack - stack method was switched to <" + stackMethod[4] + ">!", ProgressDialog.NOTIFICATION);
				}
				frames = imp.getNFrames();
				if(!onlyTimeGroup){
					//set starting timestep and ending timestep
					startGroup = 1;
					endGroup = imp.getNFrames();
				}
				
				slices = imp.getNSlices();
				int [][] impStackIndex = new int [slices][frames];
				for(int t = 0; t < frames; t++){
					for(int s = 0; s < slices; s++){
						impStackIndex [s][t] = imp.getStackIndex(1, s+1, t+1)-1;
					}
				}
				
				// For compatibility to lower versions
				Thresholder.updateCorrProperties(imp, name [task]);
				
				int xCorr = (int)Math.round(imp.getCalibration().xOrigin), 
					yCorr = (int)Math.round(imp.getCalibration().yOrigin),
					zCorr = (int)Math.round(imp.getCalibration().zOrigin);					
			//open Image
				
			//open reference Image and scale it
			   	ImagePlus primaryParImp;
			 	if(useAlternateRef){
			 		if(!new File(parDir+parName).exists()) {
			 			if(!noGUIs) {
			 				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": Could not find the parent image in the same folder. Make sure that the file <" 
			 						+ parName + "> is in the same folder as <" + name[task] + "> (so in folder " + dir[task] + ")!", ProgressDialog.ERROR);
				 			progress.moveTask(task);
			 			}	
						break running;
			 		}
			   		primaryParImp = IJ.openImage(""+parDir+parName+"");
			   	}else{
			   		parDir = dir[task];
			   		parName = name[task];
			   		primaryParImp = imp.duplicate();
			   	}
			 	primaryParImp.deleteRoi();
			 				 		
				String parImpTitle = primaryParImp.getTitle();
				
				int newWidth = (int)Math.round(scalingFactor*(double)primaryParImp.getWidth()),
					newHeight = (int)Math.round(scalingFactor*(double)primaryParImp.getHeight());
				
				ImageStack primParStack2 = new ImageStack(newWidth,newHeight); 
				ImageProcessor ip1, ip2;				
			
				for(int t = 0; t < primaryParImp.getNFrames(); t++){
					for(int z = 0; z < primaryParImp.getNSlices(); z++){
						int i = primaryParImp.getStackIndex(1,z+1,t+1);	//(int channel,int slice,int frame);
						ip1 = primaryParImp.getStack().getProcessor(i);
						String label = primaryParImp.getStack().getSliceLabel(i);
						ip1.setInterpolationMethod(ImageProcessor.BILINEAR);
						ip2 = ip1.resize(newWidth,newHeight,true);
						primParStack2.addSlice(label, ip2);	
					}
				}
				
				ImagePlus parImp = IJ.createImage(parImpTitle, width, height, stacksize, primaryParImp.getBitDepth());
				parImp.setStack(primParStack2);
				//reorder hyperstack
				if(parImp.getStackSize()>1)	parImp = HyperStackConverter.toHyperStack(parImp, 1, primaryParImp.getNSlices(), primaryParImp.getNFrames(), "default", "Color");
				primaryParImp.close();	
				System.gc();
			//open reference Image and scale it
			
				/**&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
				 *								Segmentation process
				 **&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
				
				//eventually convert to 8bit
				if(conv8Bit){
					if(!noGUIs) progress.updateBarText("converting to 8-bit...");
			 		Thresholder.optimal8BitConversion(imp, parImp);
			 		if(imp.getBitDepth() != 8 || parImp.getBitDepth() != 8){
			 			if(!noGUIs) {
			 				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": 8-bit conversion failed!", ProgressDialog.ERROR);
							progress.moveTask(task);
			 			}	
						break running;
			 		}
			 	}
				
				//initialize
				double [] thresholds = new double [imp.getStackSize()];
				Arrays.fill(thresholds, -1.0);		
				
				if(localThreshold==false){
					ImagePlus selectedImp, selectedParImp;
					if(chosenStackMethod.equals(stackMethod[0])){	
						/**
						 * AVERAGE OF SLICE THRESHOLDS
						 * */
						double averageOfAll = 0.0;
						for(int t = startGroup-1; t < endGroup; t++){
							selectedImp = getSelectedTimepoints(imp, t+1, t+1);
							selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
							double calcThreshold = this.getAverageThreshold(selectedImp, selectedParImp, 0, imp.getNSlices());
							
							//Save threshold value
							averageOfAll += calcThreshold;
							for(int s = 0; s < imp.getNSlices(); s++){
								thresholds [impStackIndex[s][t]] = calcThreshold;
							}		
							
							selectedImp.changes = false;
							selectedImp.close();
							selectedParImp.changes = false;
							selectedParImp.close();
							System.gc();
							
							if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
						}
						
						if(!separateFrames){	
							averageOfAll /= (endGroup - startGroup + 1);
							for(int t = startGroup-1; t < endGroup; t++){
								for(int s = 0; s < imp.getNSlices(); s++){
									thresholds [impStackIndex[s][t]] = averageOfAll;
								}
							}
						}	
						
					}
					
					else if(chosenStackMethod.equals(stackMethod[1])){
						/**
						 * STACK HISTOGRAM
						 * */
						if(separateFrames){							
							for(int t = startGroup-1; t < endGroup; t++){
								selectedImp = getSelectedTimepoints(imp, t+1, t+1);
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
								
								thresholds [impStackIndex[0][t]] = this.getHistogramThreshold(selectedImp, selectedParImp);
								
								//Save threshold value
								for(int s = 1; s < imp.getNSlices(); s++){
									thresholds [impStackIndex[s][t]] = thresholds [0];
								}			
								
								selectedImp.changes = false;
								selectedImp.close();
								selectedParImp.changes = false;
								selectedParImp.close();
								System.gc();
								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
						}else{
							selectedImp = getSelectedTimepoints(imp, startGroup, endGroup);
							if(onlyTimeGroup || restrictToPos){
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + startGroup,
										(int)((double)zCorr/imp.getNSlices()) + endGroup);							
							}else{
								selectedParImp = getSelectedTimepoints(parImp, 1, parImp.getNFrames());
							}
							
							thresholds [0] = this.getHistogramThreshold(selectedImp, selectedParImp);
							
							selectedImp.changes = false;
							selectedImp.close();
							selectedParImp.changes = false;
							selectedParImp.close();
							System.gc();
							
							for(int t = startGroup-1; t < endGroup; t++){
								for(int s = 0; s < imp.getNSlices(); s++){
									thresholds [impStackIndex[s][t]] = thresholds [0];
								}								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
						}
					}
					
					else if(chosenStackMethod.equals(stackMethod[2])){
						/**
						 * THRESHOLD EVERY IMAGE INDEPENDENTLY
						 * */
						if(separateFrames){
							for(int t = startGroup-1; t < endGroup; t++){
								selectedImp = getSelectedTimepoints(imp, t+1, t+1);
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
								
								//Save threshold value
								for(int s = 0; s < imp.getNSlices(); s++){
									thresholds [impStackIndex[s][t]] = this.getSingleSliceImageThreshold(selectedImp, selectedParImp, s+1);
								}			
								
								selectedImp.changes = false;
								selectedImp.close();
								selectedParImp.changes = false;
								selectedParImp.close();
								System.gc();
								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
						}else{
							selectedImp = getSelectedTimepoints(imp, startGroup, endGroup);
							selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + startGroup,
									(int)((double)zCorr/imp.getNSlices()) + endGroup);							
							
//							IJ.log("started: " + startGroup + "-" + endGroup + " " + imp.getNSlices() + "");
							for(int t = startGroup-1; t < endGroup; t++){
//								IJ.log("entered1");
								for(int s = 0; s < imp.getNSlices(); s++){
//									IJ.log("entered2");
									thresholds [impStackIndex[s][t]] //TODO
										= this.getSingleSliceImageThreshold(selectedImp, selectedParImp, 
												selectedParImp.getStackIndex(1, s+1, t-(startGroup-1)+1));
//									IJ.log("th " + t + "-" + s + ": " + thresholds [impStackIndex[s][t]]);
								}
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
							
							selectedImp.changes = false;
							selectedImp.close();
							selectedParImp.changes = false;
							selectedParImp.close();	
							System.gc();
						}
					}
					
					else if(chosenStackMethod.equals(stackMethod[3])){
						/**
						 * MAXIMUM PROJECTION
						 * */
						if(separateFrames){
							for(int t = startGroup-1; t < endGroup; t++){
								if(!noGUIs) progress.updateBarText("generate maximum projection...");
								selectedImp = getSelectedTimepoints(imp, t+1, t+1);
								selectedImp = maximumProjection(selectedImp, 1, selectedImp.getStackSize());								
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
								selectedParImp = maximumProjection(selectedParImp, 1, selectedParImp.getStackSize());
								
								thresholds [impStackIndex[0][t]] = this.getSingleSliceImageThreshold(selectedImp, selectedParImp, 1);
								
								//Save threshold value
								for(int s = 1; s < imp.getNSlices(); s++){
									thresholds [impStackIndex[s][t]] = thresholds [impStackIndex[0][t]];
								}			
								
								selectedImp.changes = false;
								selectedImp.close();
								selectedParImp.changes = false;
								selectedParImp.close();
								System.gc();
								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
						}else{
							selectedImp = getSelectedTimepoints(imp, startGroup, endGroup);
							selectedImp = maximumProjection(selectedImp, 1, selectedImp.getStackSize());
							
							if(onlyTimeGroup || restrictToPos){
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + startGroup,
										(int)((double)zCorr/imp.getNSlices()) + endGroup);		
							}else{
								selectedParImp = getSelectedTimepoints(parImp, 1, parImp.getNFrames());
							}												
							selectedParImp = maximumProjection(selectedParImp, 1, selectedParImp.getStackSize());
							
							double calculatedThreshold = this.getSingleSliceImageThreshold(selectedImp, selectedParImp, 1);
							
							for(int t = startGroup-1; t < endGroup; t++){
								for(int s = 0; s < imp.getNSlices(); s++){
									thresholds [impStackIndex[s][t]] = calculatedThreshold;
								}
								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
							
							selectedImp.changes = false;
							selectedImp.close();
							selectedParImp.changes = false;
							selectedParImp.close();
							System.gc();
						}
					}
					
					else if(chosenStackMethod.equals(stackMethod[4])){
						/**
						 * NONSTACK IMAGE
						 * */
						selectedImp = getSelectedTimepoints(imp, 1, 1);
						selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + 1,
								(int)((double)zCorr/imp.getNSlices()) + 1);							
						
						thresholds [0] = this.getSingleSliceImageThreshold(selectedImp, selectedParImp, 1);
						
						selectedImp.changes = false;
						selectedImp.close();
						selectedParImp.changes = false;
						selectedParImp.close();
						System.gc();
						
						if(!noGUIs) progress.setBar(0.5);
					}
					
					//generate binary image
					for(int z = 0; z < imp.getStackSize(); z++){
						if(thresholds [z] >= 0.0){
							this.segmentImage(imp, thresholds [z], z);
						}
						if(!noGUIs) progress.setBar(0.5 + 0.3 * (z) / imp.getStackSize());
					}
				}
				
				else{
					/***************************************************************************
					 * 								LOCAL THRESHOLD 						   *
					 ************************************************************************* */
					
					double thresholdMatrix [][][] = new double [width][height][stacksize];
					
					ImagePlus selectedParImp;
					OvalRoi localRoi = new OvalRoi(Math.round(scalingFactor*(double)(0-locThrRadius)),
							Math.round(scalingFactor*(double)(0-locThrRadius)),
							Math.round(scalingFactor*(double)(locThrRadius*2.0+1)),
							Math.round(scalingFactor*(double)(locThrRadius*2.0+1)));
					
					if(chosenStackMethod.equals(stackMethod[2]) || chosenStackMethod.equals(stackMethod[0])){
						/**
						 * THRESHOLD EVERY IMAGE INDEPENDENTLY
						 * AND
						 * AVERAGE SLICE THRESHOLDS
						 * */
						
						double average = 0.0;		
						for(int t = startGroup-1; t < endGroup; t++){
							selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
							
							//determine Threshold for every image INDEPENDENTLY
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									localRoi.setLocation(scalingFactor*(double)(x-locThrRadius),
											Math.round(scalingFactor*(double)(y-locThrRadius)));
									for(int s = 0; s < imp.getNSlices(); s++){
										thresholdMatrix [x][y][impStackIndex[s][t]] 
												= this.getThresholdOfSelection(selectedParImp, localRoi, s + 1);
									}
								}
							}
							
							//calculate AVERAGE threshold value
							if(chosenStackMethod.equals(stackMethod[0]) && separateFrames){
								for(int x = 0; x < width; x++){
									for(int y = 0; y < height; y++){
										average = 0.0;
										for(int s = 0; s < imp.getNSlices(); s++){
											average += thresholdMatrix [x][y][impStackIndex[s][t]];
										}
										average /= imp.getNSlices();
										
										for(int s = 0; s < imp.getNSlices(); s++){
											thresholdMatrix [x][y][impStackIndex[s][t]] = average;
										}
									}
								}								
							}	
							
							//close image and garbage collection
							selectedParImp.changes = false;
							selectedParImp.close();
							System.gc();
							
							if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
						}	
						
						if(chosenStackMethod.equals(stackMethod[0]) && !separateFrames){
							//calculate AVERAGE threshold value
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									average = 0.0;
									for(int z = 0; z < imp.getStackSize(); z++){
										average += thresholdMatrix [x][y][z];
									}
									average /= imp.getStackSize();
									
									for(int z = 0; z < imp.getStackSize(); z++){
										thresholdMatrix [x][y][z] = average;
									}
								}
							}
						}
					}
					
					else if(chosenStackMethod.equals(stackMethod[1])){
						/**
						 * STACK HISTOGRAM
						 * */
						if(separateFrames){							
							for(int t = startGroup-1; t < endGroup; t++){
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
								
								//determine Histogram Threshold
								for(int x = 0; x < width; x++){
									for(int y = 0; y < height; y++){
										localRoi.setLocation(scalingFactor*(double)(x-locThrRadius),
												Math.round(scalingFactor*(double)(y-locThrRadius)));
										thresholdMatrix [x][y][impStackIndex[0][t]] 
												= this.getHistogramThresholdOfSelection(selectedParImp, localRoi);
										
										for(int s = 1; s < imp.getNSlices(); s++){
											thresholdMatrix [x][y][impStackIndex[s][t]] = thresholdMatrix [x][y][impStackIndex[0][t]];
										}
									}
								}
								
								selectedParImp.changes = false;
								selectedParImp.close();
								System.gc();
								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
						}else{
							if(onlyTimeGroup){
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + startGroup,
										(int)((double)zCorr/imp.getNSlices()) + endGroup);							
							}else{
								selectedParImp = getSelectedTimepoints(parImp, 1, parImp.getNFrames());
							}
							
							//determine Histogram Threshold
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									localRoi.setLocation(scalingFactor*(double)(x-locThrRadius),
											Math.round(scalingFactor*(double)(y-locThrRadius)));
									thresholdMatrix [x][y][0] 
											= this.getHistogramThresholdOfSelection(selectedParImp, localRoi);
									
									for(int t = startGroup-1; t < endGroup; t++){
										for(int s = 1; s < imp.getNSlices(); s++){
											thresholdMatrix [x][y][impStackIndex[s][t]] = thresholdMatrix [x][y][0];
										}
									}
									
								}
								
								if(!noGUIs) progress.setBar(0.5 * (x + 1) / width);
							}
							
							selectedParImp.changes = false;
							selectedParImp.close();
							System.gc();
						}
					}
					
					else if(chosenStackMethod.equals(stackMethod[3])){
						/**
						 * MAXIMUM PROJECTION
						 * */
						if(separateFrames){
							for(int t = startGroup-1; t < endGroup; t++){								
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + t + 1, (int)((double)zCorr/imp.getNSlices()) + t + 1);
								selectedParImp = maximumProjection(selectedParImp, 1, selectedParImp.getStackSize());
								
								//determine Threshold
								for(int x = 0; x < width; x++){
									for(int y = 0; y < height; y++){
										localRoi.setLocation(scalingFactor*(double)(x-locThrRadius),
												Math.round(scalingFactor*(double)(y-locThrRadius)));
										thresholdMatrix [x][y][impStackIndex[0][t]] 
												= this.getThresholdOfSelection(selectedParImp, localRoi, 1);
										
										for(int s = 1; s < imp.getNSlices(); s++){
											thresholdMatrix [x][y][impStackIndex[s][t]] = thresholdMatrix [x][y][impStackIndex[0][t]];
										}
									}
								}
													
								selectedParImp.changes = false;
								selectedParImp.close();
								System.gc();
								
								if(!noGUIs) progress.setBar(0.5 * (t - (startGroup-1)) /(endGroup-startGroup));
							}
						}else{
							if(onlyTimeGroup){
								selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + startGroup,
										(int)((double)zCorr/imp.getNSlices()) + endGroup);		
							}else{
								selectedParImp = getSelectedTimepoints(parImp, 1, parImp.getNFrames());
							}												
							selectedParImp = maximumProjection(selectedParImp, 1, selectedParImp.getStackSize());
							
							//determine Histogram Threshold
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									localRoi.setLocation(scalingFactor*(double)(x-locThrRadius),
											Math.round(scalingFactor*(double)(y-locThrRadius)));
									thresholdMatrix [x][y][0] 
											= this.getThresholdOfSelection(selectedParImp, localRoi, 1);
									
									for(int t = startGroup-1; t < endGroup; t++){
										for(int s = 1; s < imp.getNSlices(); s++){
											thresholdMatrix [x][y][impStackIndex[s][t]] = thresholdMatrix [x][y][0];
										}
									}
									
								}
								
								if(!noGUIs) progress.setBar(0.5 * (x + 1) /(width));
							}
														
							selectedParImp.changes = false;
							selectedParImp.close();
							System.gc();
						}
					}
					
					else if(chosenStackMethod.equals(stackMethod[4])){
						/**
						 * NONSTACK IMAGE
						 * */
						selectedParImp = getSelectedTimepoints(parImp, (int)((double)zCorr/imp.getNSlices()) + 1,
								(int)((double)zCorr/imp.getNSlices()) + 1);							
						
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){
								localRoi.setLocation(scalingFactor*(double)(x-locThrRadius),
										Math.round(scalingFactor*(double)(y-locThrRadius)));
								thresholdMatrix [x][y][0] 
										= this.getThresholdOfSelection(selectedParImp, localRoi, 1);
							}
							
							if(!noGUIs) progress.setBar(0.5 * (x + 1) /(width));
						}
						
						selectedParImp.changes = false;
						selectedParImp.close();
						System.gc();
					}
					
					//generate local thresholded binary image
					this.segmentImageLocally(imp, thresholdMatrix);
				}
				parImp.changes = false;
				parImp.close();
				
				if(keepIntensities == false){
					if (record) {	
						Recorder.record = false;
					}
					IJ.run(imp, "Grays", "");
					if(imp.getBitDepth() != 8)	IJ.run(imp, "8-bit", "");
					if (record) {	
						Recorder.record = true;
					}
				}
				
				/***************************************************************************
				 *#########################################################################* 
				 * 							saving output and log						   *
				 *#########################################################################* 
				 ************************************************************************* */	
				if(autoSaveImage){
					String outputPath = this.getOutputPath(dir [task] + name [task], startDate);
					
					//save output image
					IJ.saveAs(imp, "tif", outputPath + ".tif");
					imp.changes = false;
					imp.close();
				
					//generate log file
					SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					OutputTextFile tw1 = new OutputTextFile(outputPath + "_log.txt");
					tw1.append("Output image name:	" + outputPath.substring(dir[task].length()) + ".tif");
					tw1.append("Input image name:	" + name[task]);
					tw1.append("Processing started: " + FullDateFormatter.format(startDate));
					tw1.append("");
					
					if(useAlternateRef){
						tw1.append("A REFERENCE IMAGE was used for threshold calculation");
						tw1.append("	Reference image name:	" + parName);
						tw1.append("");
						
						if(restrictToPos){
							tw1.append("	Threshold calculation in the reference image was restricted to size and position of the input image.");
							tw1.append("");
						}
						
						tw1.append("	ROI coordinates were corrected in the reference image");
						tw1.append("		X correction:	" + xCorr);
						tw1.append("		Y correction:	" + yCorr);
						tw1.append("		Z correction:	" + zCorr);
						tw1.append("");
					}
					
					
					tw1.append("PROCESSING OPTIONS");
					tw1.append("	Autothreshold method:	" + selectedAlgorithm);
					tw1.append("	Scaling factor (x/y dimension):	" + dformat2.format(scalingFactor));
					if(conv8Bit){
						tw1.append("	Images were converted to 8-bit before processing.");
					}
					if(fillHoles){
						tw1.append("	Holes in mask were closed.");
					}					
					if(keepIntensities){
						tw1.append("	A background-removed image was created (where intensity values of foreground pixels are kept)");
					}
					if(separateFrames){
						tw1.append("	Threshold every time-step independently.");
					}
					if(onlyTimeGroup){
						tw1.append("	Threshold only distinct time-series:	" + startGroup + " - " + endGroup);
					}
					tw1.append("");
					
					if(!localThreshold){
						tw1.append("	Stack handling:	" + chosenStackMethod);
						tw1.append("	Table of applied thresholds");
						tw1.append("		slice-image	threshold");
						for(int z = 0; z < stacksize; z++){
							if(thresholds[z] >= 0.0){
								tw1.append("		" + (z+1) + "	" + dformat0.format((int)Math.round(thresholds[z])));
							}
						}
					}else{
						tw1.append("	Every specifc x,y position was thresholded independently (local threshold):");
						tw1.append("	Local threshold radius	" + dformat0.format(locThrRadius));
						tw1.append("	Stack handling:	" + chosenStackMethod);
					}

					tw1.append("");
					tw1.append("Datafile was generated by '"+PLUGINNAME+"_.java', \u00a9 2015 - " + yearOnly.format(new Date()) + " Jan Niklas Hansen (jan.hansen@caesar.de).");
					tw1.append("The plugin '"+PLUGINNAME+"_.java' is distributed in the hope that it will be useful,"
							+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
							+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
					tw1.append("Plugin version:	"+PLUGINVERSION);
					if (record) {	
						Recorder.record = false;
					}
					tw1.finish();
					if (record) {	
						Recorder.record = true;
					}
				}
				//Save Image Treatment
				
				//Update Multi-Task Manager
				if(!noGUIs) {
					progress.setBar(1.0);
					progress.moveTask(task);
				}
				break running;
		}
		System.gc();
	}
	processingDone = true;	
}


private static void updateCorrProperties(ImagePlus imp, String name) {
	/**
	 * In former versions of the MotiQ Thresholder data for X,Y,Z correction were derived from the images name.
	 * To ensure downwards compatibility, this method overwrites the image-properties for XYZ correction with data
	 * derived from the images name, if there are no defined values for XYZ correction in the image-properties.
	 * <imp> image
	 * <name> = image file name
	 * */
	
	//only if there are no settings saved, the correction data is derived from the name
	if(imp.getCalibration().xOrigin == 0.0
			&& imp.getCalibration().yOrigin == 0.0
			&& imp.getCalibration().zOrigin == 0.0){
	
		if(name.contains("_X")&&name.contains("_Y")&&name.contains("_Z")){
			int endCoPos = name.indexOf("_Z")+2;
			int anyInt;
			while(true){				
				try {
					anyInt = Integer.parseInt(name.substring(name.indexOf("_Z")+2,endCoPos+1));
				}catch (Exception e) {
					break;
				}
				endCoPos++;
			}

			imp.getCalibration().xOrigin = Integer.parseInt(name.substring(name.indexOf("_X")+2,name.indexOf("_Y")));
			imp.getCalibration().yOrigin = Integer.parseInt(name.substring(name.indexOf("_Y")+2,name.indexOf("_Z")));
			imp.getCalibration().zOrigin = Integer.parseInt(name.substring(name.indexOf("_Z")+2,endCoPos));				
		}
	}
}

private static void optimal8BitConversion (ImagePlus imp, ImagePlus parImp){
	//set displayrange from minimum to maximum and then convert
	double min = Double.POSITIVE_INFINITY, max = 0.0;
	for(int x = 0; x < imp.getWidth(); x++){
		for(int y = 0; y < imp.getHeight(); y++){
			for(int z = 0; z < imp.getStackSize(); z++){
				if(imp.getStack().getVoxel(x, y, z)>max){
					max = imp.getStack().getVoxel(x, y, z);
				}
				if(imp.getStack().getVoxel(x, y, z)<min){
					min = imp.getStack().getVoxel(x, y, z);
				}
			}			
		}
	}
	for(int x = 0; x < parImp.getWidth(); x++){
		for(int y = 0; y < parImp.getHeight(); y++){
			for(int z = 0; z < parImp.getStackSize(); z++){
				if(parImp.getStack().getVoxel(x, y, z)>max){
					max = parImp.getStack().getVoxel(x, y, z);
				}
				if(parImp.getStack().getVoxel(x, y, z)<min){
					min = parImp.getStack().getVoxel(x, y, z);
				}
			}			
		}
	}
	
	imp.setDisplayRange(min, max);
	parImp.setDisplayRange(min, max);
	
	if(imp.getBitDepth() != 8){
		ImageConverter impConv = new ImageConverter(imp);
		impConv.convertToGray8();
		impConv = new ImageConverter(parImp);
		impConv.convertToGray8();
	}
}

/**
 * @return a scaled a PolygonRoi @param roi by the factor @param scaling
 * */
private static PolygonRoi scaleRoi(Roi roi, double scaling){
	Polygon subimagePolygon = roi.getConvexHull();
	int [] subXPoints = subimagePolygon.xpoints;
	int [] subYPoints = subimagePolygon.ypoints;
	for(int i = 0; i < subXPoints.length; i++){
		subXPoints [i] = (int)Math.round((double)subXPoints [i]*scaling);
		subYPoints [i] = (int)Math.round((double)subYPoints [i]*scaling);
	}
	subimagePolygon = new Polygon(subXPoints, subYPoints, subXPoints.length);
	return (new PolygonRoi(subimagePolygon,ij.gui.Roi.POLYGON));
}

/**
 * @return ROI circumscribing all pixels of the ImagePlus imp with an intensity above 0.0
 * sliceImage range: 1 < sliceImage < Stacksize 
 * */
PolygonRoi getPositionRoi(ImagePlus imp, int startSliceImage, int endSliceImage){
	Polygon selectionPolygon = new Polygon();
	int xCorr = (int)Math.round(imp.getCalibration().xOrigin);
	int yCorr = (int)Math.round(imp.getCalibration().yOrigin);
	
	for(int x = 0; x < imp.getWidth(); x++){
		for(int y = 0; y < imp.getHeight(); y++){
			searching: for(int z = startSliceImage; z <= endSliceImage; z++){
				if(imp.getStack().getVoxel(x,y,z-1) > 0.0){						
					selectionPolygon.addPoint(x + xCorr,y + yCorr);
					break searching;
				}
			}
			if(selectionPolygon.npoints > 2000){
				selectionPolygon = new PolygonRoi(selectionPolygon,Roi.POLYGON).getConvexHull();
			}
		}				
	}
	
	if(selectionPolygon.npoints>0){
		selectionPolygon = new PolygonRoi(selectionPolygon,Roi.POLYGON).getConvexHull();
		return new PolygonRoi(selectionPolygon,Roi.POLYGON);
	}else{
		return null;
	}	
}

/**
 * @return maximum-intensity-projection image of the specified stack range in the input ImagePlus (imp)
 * startSlice = first slice included into projection (1 < startSlice < NSlices)
 * endSlice = last slice included into projection (1 < endSlice < NSlices)
 * */
private static ImagePlus maximumProjection(ImagePlus imp,int startSlice,int endSlice){
	//reset borders, if indicated start / end does not fit stack size
	if(startSlice < 1)	startSlice=1;
	if(endSlice > imp.getStackSize())	endSlice = imp.getStackSize();
	
	//generate maximum intensity projection
	ImagePlus transImp = IJ.createImage("MIP", imp.getWidth(), imp.getHeight(), 1, imp.getBitDepth());
	double maximumMeasured;
	for(int x = 0; x < imp.getWidth(); x++){
		for(int y = 0; y < imp.getHeight(); y++){
			maximumMeasured = 0.0;
			for(int z = startSlice-1; z < endSlice; z++){
				if(imp.getStack().getVoxel(x,y,z) > maximumMeasured){
					maximumMeasured = imp.getStack().getVoxel(x,y,z);
				}
			}
			transImp.getStack().setVoxel(x,y,0,maximumMeasured);
		}
	}
	transImp.setCalibration(imp.getCalibration());
	return transImp;
}

/**
 * @return a new ImagePlus exlusively containing the selected <timepoint> of the ImagePlus <imp>
 * Range of <timepoint>: 1 <= timepoint <= imp.getNFrames()
 * */
private static ImagePlus getSelectedTimepoints (ImagePlus imp, int firstTimepoint, int lastTimepoint){
	ImagePlus transImp = IJ.createHyperStack("Selected Timepoints", imp.getWidth(), imp.getHeight(),
			imp.getNChannels(), imp.getNSlices(), lastTimepoint-firstTimepoint+1, imp.getBitDepth());
	
	for(int t = firstTimepoint; t <= lastTimepoint; t++){
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				for(int s = 0; s < imp.getNSlices(); s++){
					int zOld = imp.getStackIndex(1, (s+1), t) - 1;
					int zNew = transImp.getStackIndex(1, s+1, t - firstTimepoint + 1) - 1;
					transImp.getStack().setVoxel(x, y, zNew, imp.getStack().getVoxel(x, y, zOld));	
				}
			}
		}
	}	
	transImp.setCalibration(imp.getCalibration());
	
	//TODO check whether true
	//imp.show();
	//transImp.show();
	
	//new WaitForUserDialog("Is this sequence " + firstTimepoint + "-" + lastTimepoint + "?").show();
	//imp.hide();
	//transImp.hide();
	
	return transImp;
}

/**
 * Determines a threshold in the ImagePlus @param parImp at the position of the ImagePlus @param imp by setting a threshold 
 * that is the AVERAGE of the thresholds determined each in another slice image of <parImp>
 * Only the slice images between the indicated @param startSliceImage and @param endSliceImage are included into calculation
 * sliceImage range: 1 <= sliceImage <= stacksize
 * */
private double getAverageThreshold (ImagePlus imp, ImagePlus parImp, int startSliceImage, int endSliceImage){
	if(!noGUIs) progress.updateBarText("get average threshold...");
	
	//initialize
	double calculatedThreshold = 0.0;
		
	Roi selection = new Roi(0,0,imp.getWidth(), imp.getHeight());
	if(restrictToPos){
		selection = this.getPositionRoi(imp, startSliceImage, endSliceImage);
	}	
	selection = scaleRoi(selection, scalingFactor);
	
	//calculate thresholds
	for(int z = startSliceImage; z <= endSliceImage; z++){
		parImp.setSlice(z+(int)Math.round(imp.getCalibration().zOrigin));
		parImp.deleteRoi();
		if(restrictToPos) parImp.setRoi(selection);
		
		parImp.setSlice(z);
		parImp.getProcessor().setSliceNumber(z);
		IJ.setAutoThreshold(parImp, (selectedAlgorithm+" dark"));
		parImp.getProcessor().setSliceNumber(z);
		calculatedThreshold += parImp.getProcessor().getMinThreshold();
//		IJ.log("threshold at z " + z + "  = " + parImp.getProcessor().getMinThreshold()); //TODO check for functionality
	}					
	return calculatedThreshold / (double)(endSliceImage-startSliceImage+1);
}

/**
 * Calculates a threshold in the histogram of ImagePlus <parImp> for the image <imp>
 * Only the slice images between the indicated <startSliceImage> and <endSliceImage> are included into calculation
 * */
private double getHistogramThreshold (ImagePlus imp, ImagePlus parImp){
	if(!noGUIs) progress.updateBarText("get histogram threshold...");
	
	parImp.deleteRoi();
	if(restrictToPos){
		Roi selection = this.getPositionRoi(imp, 1, imp.getStackSize());
		selection = scaleRoi(selection, scalingFactor);
		parImp.setRoi(selection);		
	}
	
	//calculate thresholds	
	IJ.setAutoThreshold(parImp, (selectedAlgorithm+" dark stack"));
	return parImp.getProcessor().getMinThreshold();
}

/**
 * @return a threshold for the slice image <s> in the ImagePlus <parImp> for the image <imp>
 * range: 1 <= z <= stacksize
 * */
private double getSingleSliceImageThreshold (ImagePlus imp, ImagePlus parImp, int s){
	if(!noGUIs) progress.updateBarText("determine threshold...");
	parImp.deleteRoi();
	if(restrictToPos){
		if(!noGUIs) progress.updateBarText("find position...");
		Roi selection = this.getPositionRoi(imp, 1, imp.getStackSize());
		selection = scaleRoi(selection, scalingFactor);
		parImp.setRoi(selection);		
	}
	
	//calculate thresholds	
	parImp.setSlice(s);
	parImp.getProcessor().setSliceNumber(s);
	IJ.setAutoThreshold(parImp, (selectedAlgorithm+" dark"));
	parImp.getProcessor().setSliceNumber(s);
//	IJ.log("th " + s + ": " + parImp.getProcessor().getMinThreshold());
//	parImp.getProcessor().setAutoThreshold(arg0, arg1, arg2);	//TODO check whether here ignore black is possible?
	return parImp.getProcessor().getMinThreshold();
}


private double getThresholdOfSelection(ImagePlus parImp, Roi selection, int z){
	parImp.setRoi(selection);
	parImp.setSlice(z);
	parImp.getProcessor().setSliceNumber(z);
	IJ.setAutoThreshold(parImp, (selectedAlgorithm+" dark"));
	parImp.getProcessor().setSliceNumber(z);
//	parImp.getProcessor().setAutoThreshold(arg0, arg1, arg2);	//TODO check whether here ignore black is possible?
	return parImp.getProcessor().getMinThreshold();
}

/**
 * @return a threshold for the histogram of ImagePlus <parImp> within the <selection>
 * */
private double getHistogramThresholdOfSelection (ImagePlus parImp, Roi selection){
	if(!noGUIs) progress.updateBarText("determine histogram threshold...");
	parImp.deleteRoi();
	parImp.setRoi(selection);
	
	//calculate thresholds	
	IJ.setAutoThreshold(parImp, (selectedAlgorithm+" dark stack"));
	return parImp.getProcessor().getMinThreshold();
}


private void segmentImage(ImagePlus imp, double threshold, int z){
	//z=slicenr
	int maxValue = getMaxPossibleIntensity(imp);	
	if(fillHoles){
		if(!noGUIs) progress.updateBarText("filling holes...");
		//generate a mask image
		ImagePlus maskImp = IJ.createImage("Mask", "8-bit", imp.getWidth(), imp.getHeight(), 1);
		double imageMax = getMaxPossibleIntensity(maskImp);
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				if(imp.getStack().getVoxel(x,y,z) >= threshold){
					maskImp.getStack().setVoxel( x, y, 0, imageMax);
				}else{
					maskImp.getStack().setVoxel( x, y, 0, 0.0);
				}
			}
		}
		imp.setPosition(z+1);
		
		if (record) {	
			Recorder.record = false;
		}
		if(!Prefs.blackBackground) {
			IJ.run(maskImp, "Invert", "slice");
		}
		IJ.run(maskImp, "Fill Holes", "slice");
		if(!Prefs.blackBackground) {
			IJ.run(maskImp, "Invert", "slice");
		}
		if (record) {	
			Recorder.record = true;
		}
		
		if(!noGUIs) progress.updateBarText("segment image...");
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				if(maskImp.getStack().getVoxel(x,y,0) == 0.0){
					imp.getStack().setVoxel( x, y, z, 0.0);
				}else if(keepIntensities == false){
					imp.getStack().setVoxel( x, y, z, maxValue);
				}
			}
		}
		maskImp.changes = false;
		maskImp.close();
	}else{
		if(!noGUIs) progress.updateBarText("segment image...");
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				double pxintensity = imp.getStack().getVoxel(x,y,z);
				if(pxintensity < threshold){
					imp.getStack().setVoxel( x, y, z, 0.0);
				}else if(keepIntensities == false){
					imp.getStack().setVoxel( x, y, z, maxValue);
				}
			}
		}
	}		
}


private void segmentImageLocally (ImagePlus imp, double thresholdMatrix [][][]){
	int maxIntensity = getMaxPossibleIntensity(imp);
	if(fillHoles){
		//Include fill Holes mechanism
		if(!noGUIs) progress.updateBarText("filling holes...");
		ImagePlus transImp = IJ.createHyperStack("Trans Imp", imp.getWidth(), imp.getHeight(), 1,
				imp.getNSlices(), imp.getNFrames(), imp.getBitDepth());
		
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				for(int z = 0; z < imp.getStackSize(); z++){
					if(thresholdMatrix[x][y][z]==0.0 
							|| imp.getStack().getVoxel(x,y,z) < (int)Math.round(thresholdMatrix[x][y][z])){
						transImp.getStack().setVoxel(x, y, z, 0.0);
					}else{
						transImp.getStack().setVoxel( x, y, z, maxIntensity);
					}
				}				
			}
		}
		
		if (record) {	
			Recorder.record = false;
		}
		if(!Prefs.blackBackground) {
			IJ.run(transImp, "Invert", "stack");
		}	
		
		IJ.run(transImp, "Fill Holes", "stack");
				
		if(!Prefs.blackBackground) {
			IJ.run(transImp, "Invert", "stack");
		}
		if (record) {	
			Recorder.record = true;
		}
		
		if(!noGUIs) progress.updateBarText("segment image...");
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				for(int z = 0; z < imp.getStackSize(); z++){
					if(transImp.getStack().getVoxel(x,y,z) == 0.0
							&& thresholdMatrix[x][y][z] != 0.0){
						imp.getStack().setVoxel( x, y, z, 0.0);
					}else if(keepIntensities == false){
						imp.getStack().setVoxel( x, y, z, maxIntensity);
					}
				}				
			}
			
			if(!noGUIs) progress.setBar(0.5 + 0.3 * (x + 1) /(imp.getWidth()));
		}
		transImp.changes = false;
		transImp.close();
	}else{
		if(!noGUIs) progress.updateBarText("segment image...");
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){
				for(int z = 0; z < imp.getStackSize(); z++){
					if(imp.getStack().getVoxel(x,y,z) < (int)Math.round(thresholdMatrix [x][y][z])
							|| thresholdMatrix [x][y][z]==0.0){
						imp.getStack().setVoxel(x, y, z, 0.0);
					}else if(keepIntensities == false){
						imp.getStack().setVoxel(x, y, z, maxIntensity);
					}
				}				
			}
			
			if(!noGUIs) progress.setBar(0.5 + 0.3 * (x + 1) /(imp.getWidth()));
		}
	}		
}


private int getMaxPossibleIntensity(ImagePlus imp){
	int maxThreshold = 0;
	if(imp.getBitDepth()==8){
		maxThreshold = 255;
	}else if(imp.getBitDepth()==16){
		maxThreshold = 65535;
	}else if(imp.getBitDepth()==32){
		maxThreshold = 2147483647;
	}else{
		if(!noGUIs) progress.notifyMessage("Error! No gray scale image!", ProgressDialog.ERROR);
	}
	return maxThreshold;	
}

private String getOutputPath(String path, Date d){
	if(!noGUIs) progress.updateBarText("determine output path...");
	SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");

	String name = path.substring(path.lastIndexOf(System.getProperty("file.separator"))+1);
	if(name.contains(".")){
		//Remove type-ending from name
		name = name.substring(0, name.lastIndexOf("."));
	}
					
	String outputPath = ""+path.substring(0, path.lastIndexOf(System.getProperty("file.separator"))+1)+name+"";
	if(localThreshold){
		outputPath += "l";
	}
	if(keepIntensities){
		outputPath += "_pBIN";
	}else{
		outputPath += "_BIN";
	}				
	if(saveDate){				
		outputPath += "_" + NameDateFormatter.format(d);
	}
	return outputPath;
}
}