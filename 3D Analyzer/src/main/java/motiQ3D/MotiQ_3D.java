/***===============================================================================
 * 
 * MotiQ_3D Version plugin for ImageJ
 * 
 * Copyright (C) 2014-2023 Jan N. Hansen
 * First version: July 28, 2014 
 * This Version: January 25, 2023
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
 * ===========================================================================**/

package motiQ3D;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.UIManager;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.text.*;
import java.text.*;

public class MotiQ_3D implements PlugIn, Measurements{
	//Name variables
	static final String PLUGINNAME = "MotiQ 3D Analyzer";
	static final String PLUGINVERSION = "v0.2.0";
	
	DecimalFormat dformat6 = new DecimalFormat("#0.000000");
	DecimalFormat dformat3 = new DecimalFormat("#0.000");
	DecimalFormat dformat0 = new DecimalFormat("#0");
	DecimalFormat dformatdialog = new DecimalFormat("#0.000000");
	
	static final SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
	static final SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
	static final SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd	HH:mm:ss");
	static final SimpleDateFormat FullDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static final SimpleDateFormat Time = new SimpleDateFormat("mm:ss");
	Date currentDate = new Date();
	
	//Fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	
	//-----------------define numbers for Dialog-----------------
	static final String[] taskVariant = {"the active, open image",
			"multiple images (use multi-task manager to open images)",
			"all open images"};
	String selectedTaskVariant = taskVariant[1];
	int tasks = 1;
	
	boolean recalibrate = true;
	double calibration = 0.417;
	String calibrationDimension = "um"; //"\u03BCm";
	double voxelDepth = 1.0;
	double timePerFrame = 0.5;
	static final String[] timeFormats = {"sec", "min", "hr", "d"};
	String timeUnit = timeFormats[1];
	int totalGroupSize = 10;

	double minParticleVolume = 50;
	boolean onlyLargest = false;
	boolean semiBinary = true;
	
	static final String[] mergeOrNotMerge = {"all detected particles merged into one object","every particle seperately (track particles by overlap)"};
	String mergeSelection = mergeOrNotMerge [0];
	
	static final String [] sklOptions = {"do not calculate skeleton results", "derive skeleton from a copy of the input image", "derive skeleton data from a BINARIZED copy of the input image"};
	String sklOptionSelection = sklOptions [1];
	boolean skeletonize = true;
	boolean binarizeBeforeSkl = true;
	double gSigmaXY = 1.0,
			gSigmaZ = 0.0;
	
	boolean saveDate = false;
		
	//choice   Number Format for Saving-------------------------------------------	
		static final String[] nrFormats = {"US (0.00...)", "Germany (0,00...)"};
		String ChosenNumberFormat = nrFormats[0];
	//choice   Number Format for Saving-------------------------------------------
	
	ProgressDialog progress;		
	//-----------------define numbers for Dialog-----------------

	boolean continueProcessing = true;
	boolean allTasksDone = false;
	
	Visualizer3D v3D;
	
public void run(String arg) {
	dformatdialog.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	//-------------------------GenericDialog--------------------------------------
	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	GenericDialog gd = new GenericDialog(PLUGINNAME + " - settings");
	//show Dialog-----------------------------------------------------------------
//	gd.setInsets(0,0,0); (top, left, bottom)
	gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", version " + PLUGINVERSION 
			+ " (\u00a9 2014 - " + yearOnly.format(new Date()) + ", Jan N. Hansen)", SuperHeadingFont);
//	gd.setInsets(-5,0,0);	gd.addMessage("WARNING: This version is not compatible with versions of Jan 2016 or earlier!", InstructionsFont);
	
//	gd.setInsets(5,0,0);	gd.addMessage("Input image location", SubHeadingFont);
	gd.setInsets(5,0,0);	gd.addChoice("Process ", taskVariant, selectedTaskVariant);
	
	gd.setInsets(5,0,0);	gd.addMessage("Calibration", SubHeadingFont);
	gd.setInsets(5,0,0);	gd.addCheckbox("Re-calibrate image:", recalibrate);
	gd.setInsets(0,0,0);	gd.addNumericField("Length calibration [calibration unit/px]: ", calibration, 4);
	gd.setInsets(0,0,0);	gd.addNumericField("Voxel depth [calibration unit/voxel]: ", voxelDepth, 4);
	gd.setInsets(0,0,0);	gd.addStringField("Calibration unit: ", calibrationDimension);
	gd.setInsets(5,0,0);	gd.addNumericField("Time interval: [time unit]: ", timePerFrame, 2);
	gd.setInsets(0,0,0);	gd.addChoice("Time unit: ", timeFormats, timeUnit);
		
	gd.setInsets(5,0,0);	gd.addMessage("Particle filtering", SubHeadingFont);
	gd.setInsets(0,0,0);	gd.addNumericField("Minimum particle volume [voxel]: ",  minParticleVolume, 0);
	gd.setInsets(0,0,0);	gd.addCheckbox("In each time-step, remove all particles but the largest one.", onlyLargest);
	
	gd.setInsets(5,0,0);	gd.addMessage("Calculation", SubHeadingFont);
	gd.setInsets(5,0,0);	gd.addNumericField("# time-steps grouped for long-term analysis: ", totalGroupSize, 0);
	gd.setInsets(0,0,0);	gd.addChoice("Calculate results for", mergeOrNotMerge, mergeSelection);
	gd.setInsets(5,0,0);	gd.addChoice("SKELETON options: ", sklOptions, sklOptionSelection);
	gd.setInsets(5,0,0);	gd.addNumericField("Gauss filter prior to skeletonization - sigma XY and Z: ", gSigmaXY, 1);
	gd.setInsets(-23,60,0);	gd.addNumericField("", gSigmaZ, 1);
	
	gd.setInsets(5,0,0);	gd.addMessage("Output", SubHeadingFont);
	gd.setInsets(5,0,0);	gd.addChoice("Number format", nrFormats, ChosenNumberFormat);
	gd.setInsets(0,0,0);	gd.addCheckbox("Include date/time of analysis in output file-names", saveDate);
	
	gd.showDialog();
	//show Dialog-----------------------------------------------------------------

	//show Dialog-----------------------------------------------------------------
	
	selectedTaskVariant = gd.getNextChoice();
	
	recalibrate = gd.getNextBoolean();
	calibration = (double) gd.getNextNumber();
	voxelDepth = (double) gd.getNextNumber();
	calibrationDimension = gd.getNextString();
	timePerFrame = (double) gd.getNextNumber();
	if(timePerFrame==0.0){
		timePerFrame=1.0;
	}
	timeUnit = gd.getNextChoice();
		
	minParticleVolume = (int) gd.getNextNumber();
	onlyLargest = gd.getNextBoolean();
	
	totalGroupSize = (int) gd.getNextNumber();
	mergeSelection = gd.getNextChoice();
	
	sklOptionSelection = gd.getNextChoice();
	if(sklOptionSelection.equals(sklOptions [0])){
		skeletonize = false;
	}else if(sklOptionSelection.equals(sklOptions [1])){
		skeletonize = true;
		binarizeBeforeSkl = false;		
	}else if(sklOptionSelection.equals(sklOptions [2])){
		skeletonize = true;
		binarizeBeforeSkl = true;
	}
	gSigmaXY = (double) gd.getNextNumber();
	gSigmaZ = (double) gd.getNextNumber();
		
	ChosenNumberFormat = gd.getNextChoice();
	if(ChosenNumberFormat.equals(nrFormats[0])){
		dformat0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformat3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformat6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	}else if (ChosenNumberFormat.equals(nrFormats[1])){
		dformat0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
		dformat3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
		dformat6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
	}
	saveDate = gd.getNextBoolean();
	
	if (gd.wasCanceled()) return;	
	
/**&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
							load image tasks
&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	
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
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				return;
			}
			FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
			try {
				name [0] = info.fileName;	//get name
				dir [0] = info.directory;	//get directory		
			}catch(Exception e) {
				IJ.error(PLUGINNAME + " cannot retrieve where the image " + WindowManager.getCurrentImage().getTitle() + "is saved. Save the image and relaunch " + PLUGINNAME + ".");
				return;
			}
			
			tasks = 1;
		}else if(selectedTaskVariant.equals(taskVariant[2])){	// all open images
			if(WindowManager.getIDList()==null){
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
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
					IJ.error(PLUGINNAME + " cannot retrieve where the image " + WindowManager.getCurrentImage().getTitle() + "is saved. Save the image and relaunch " + PLUGINNAME + ".");
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
						IJ.error(PLUGINNAME + " cannot retrieve where the image " + allImps[i].getTitle() + "is saved. Save the image and relaunch " + PLUGINNAME + ".");
						return;
					}
				}		
			}
					
		}
		
		//add progressDialog
		progress = new ProgressDialog(name, tasks);
		progress.setLocation(0,0);
		progress.setVisible(true);
		progress.addWindowListener(new java.awt.event.WindowAdapter() {
	        public void windowClosing(WindowEvent winEvt) {
	        	if(allTasksDone==false){
	        		IJ.error("Script stopped...");
	        	}
	        	v3D = null;
	        	continueProcessing = false;	        	
	        	return;
	        }
		});
		
/**&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
							Open image
&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	continueAll: while (continueProcessing){
	for(int task = 0; task < tasks; task++){
		if(continueProcessing == false){
			break continueAll;
		}
		Date startDate = new Date();
		progress.updateBarText(" in progress...");
		running: while(true){
		//Check for problems with image file
			if(name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".txt")){
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": File is no image! Could not be processed!",ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}
			if(name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".zip")){	
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": File is no image! Could not be processed!",ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}
		//Check for problems with image file
						
		//open Image
		   	ImagePlus imp;
		   	try{
		   		if(selectedTaskVariant.equals(taskVariant[1])){
		   			imp = IJ.openImage(""+dir[task]+name[task]+"");			   			
					imp.deleteRoi();
		   		}else if(selectedTaskVariant.equals(taskVariant[0])){
		   			imp = WindowManager.getCurrentImage();
		   			imp.deleteRoi();
		   		}else{
		   			imp = allImps[task];
		   			imp.deleteRoi();
		   		}
		   	}catch (Exception e) {
		   		progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": file is no image - could not be processed!",ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}
		   	
			int width = imp.getWidth();
			int height = imp.getHeight();
			int frames = imp.getNFrames();
			int slices = imp.getNSlices();
		//open Image

		/***************************************************************************
		 *#########################################################################* 
		 * 								ANALYSIS								   *
		 *#########################################################################* 
		 ************************************************************************* */		
			
			//Calibrate
			Calibration cal = imp.getCalibration();
			double pixelWidth = cal.pixelWidth;
			double pixelHeight = cal.pixelHeight; 
			double pixelDepth = cal.pixelDepth;
			double fps = cal.fps;
			double frameInterval = cal.frameInterval;

			double xCorr = imp.getCalibration().xOrigin, 
				yCorr = imp.getCalibration().yOrigin,
				zCorr = imp.getCalibration().zOrigin;
			
			//for downwards compatibility
			if(xCorr == 0.0 && yCorr == 0.0 && zCorr == 0.0 
					&& name [task].contains("_X") && name [task].contains("_Y") & name[task].contains("_Z")){
				int xCoPos = name [task].indexOf("_X");
				int yCoPos = name [task].indexOf("_Y");
				int zCoPos = name [task].indexOf("_Z");
				int endCoPos = zCoPos+2;
				while(true){				
					try {
						int anyInt = Integer.parseInt(name [task].substring(zCoPos+2,endCoPos+1));
					}catch (Exception e){
						break;
					}
					endCoPos++;
				}
//				int endCoPos = imageName.indexOf("_BIN");
				xCorr=Integer.parseInt(name [task].substring(xCoPos+2,yCoPos));
				yCorr=Integer.parseInt(name [task].substring(yCoPos+2,zCoPos));
				zCorr=Integer.parseInt(name [task].substring(zCoPos+2,endCoPos));			
			}
			
			String unit = cal.getUnit();
			String calTimeUnit = cal.getTimeUnit();
			if(recalibrate){
				pixelWidth = calibration;		cal.pixelWidth = pixelWidth;
				pixelHeight = calibration;		cal.pixelHeight = pixelHeight;			
				unit = calibrationDimension;	cal.setUnit(unit);
				pixelDepth = voxelDepth;		cal.pixelDepth = pixelDepth;
				frameInterval = timePerFrame;	cal.frameInterval = frameInterval;
				calTimeUnit = timeUnit; 		cal.setTimeUnit(calTimeUnit);
				if(timeUnit.equals(timeFormats[0])){
					fps = 1.0 / (double)timePerFrame;
					cal.fps = fps;
				}else if(timeUnit.equals(timeFormats[1])){
					fps = 1.0 / (60*(double)timePerFrame);
					cal.fps = fps;
				}else if(timeUnit.equals(timeFormats[2])){
					fps = 1.0 / (60*60*(double)timePerFrame);
					cal.fps = fps;
				}else{
					fps = 1.0 / (24*60*60*(double)timePerFrame);
					cal.fps = fps;
				}
			}else{
				if(pixelWidth==pixelHeight){
					calibration = pixelWidth;
					voxelDepth = pixelDepth;
					calibrationDimension = ""+unit;
					timePerFrame = frameInterval;
					if(timePerFrame==0.0){timePerFrame=1.0;}
					cal.frameInterval = timePerFrame;
					if(fps==0.0){fps = 1.0;}
					cal.fps = fps;
					timeUnit = calTimeUnit;
				}else{
					progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": recalibration failed! Metadata calibration is used instead!!",ProgressDialog.ERROR);
				}
			}			
			//Calibrate
			
			//Investigate image
			double minDetectedNonZeroIntensity = Double.POSITIVE_INFINITY;	//For auto-detection of binary or non-binary image
			
			int pxCount = 0;
			int [] pxCountPF = new int [frames];
			for(int t = 0; t < frames; t++){
				pxCountPF[t] = 0;
				for(int z = 0; z < slices; z++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){							
							if(imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1) > 0.0){								
								if(imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1) < minDetectedNonZeroIntensity){
									minDetectedNonZeroIntensity = imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1);
								}
								pxCount++;
								pxCountPF[t]++;
							}						
						}
					}
				}				
			}
			//Investigate image
			
			//Check whether image contains intensity informations
				if(minDetectedNonZeroIntensity!=(double)getMaxIntensity(imp)){
					semiBinary = true;
				}else{
					semiBinary = false;
				}
			
			//Filter particles
			int excludedParticles [] = new int [frames];
			int includedParticles = 0;
			final double ProgressFactor0 = 0.15;
			final double ProgressFactor1 = 0.15;
			int floodNodeX, floodNodeY, floodNodeZ, floodNodeT, index = 0;
			int[][] floodNodes = new int[pxCount][4];
			{				
				int newPxCount = pxCount;
				int pxCount100 = pxCount/100; if (pxCount100==0){pxCount100 = 1;}
				int pxCount1000 = pxCount/1000; if (pxCount1000==0){pxCount1000 = 1;}
				int processedPxCount = 0;				
				
				ArrayList <ImPoint> pointSaveCollection;
				ArrayList <ImPoint> pointCollection;
				
				searchCells: for(int t = 0; t < frames; t++){
					pointSaveCollection = new ArrayList <ImPoint> (pxCountPF[t]);
					pointCollection = new ArrayList <ImPoint> (pxCountPF[t]);
					excludedParticles [t] = 0;
					int newPxCountPF = pxCountPF [t] - excludedParticles [t];
					for(int z = 0; z < slices; z++){
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){		
								if(imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1) > 0.0){
									pointCollection.clear();
									pointCollection.ensureCapacity(newPxCountPF);
									pointCollection.add(new ImPoint(x,y,z,t,imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1)));
									imp.getStack().setVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1,0.0);
									processedPxCount++;
									
									//Floodfiller					
									floodNodeX = x;
									floodNodeY = y;
									floodNodeZ = z;
									floodNodeT = t;
									index = 0;
									floodNodes[0][0] = floodNodeX;
									floodNodes[0][1] = floodNodeY;
									floodNodes[0][2] = floodNodeZ;
									floodNodes[0][3] = floodNodeT;
									while (index >= 0){
										floodNodeX = floodNodes[index][0];
										floodNodeY = floodNodes[index][1];
										floodNodeZ = floodNodes[index][2];
										floodNodeT = floodNodes[index][3];
										index--;            
										if ((floodNodeX > 0) 
												&& (imp.getStack().getVoxel(floodNodeX-1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX-1),(floodNodeY),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX-1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX-1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1,0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX-1;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeX < (width-1)) 
												&& (imp.getStack().getVoxel(floodNodeX+1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX+1),(floodNodeY),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX+1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX+1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX+1;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeY > 0) 
												&& (imp.getStack().getVoxel(floodNodeX, floodNodeY-1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY-1),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX, floodNodeY-1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX, floodNodeY-1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY-1;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}                
										if ((floodNodeY < (height-1)) 
												&& (imp.getStack().getVoxel(floodNodeX, floodNodeY+1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY+1),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX, floodNodeY+1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX, floodNodeY+1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1,0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY+1;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeZ > 0) 
												&& (imp.getStack().getVoxel(floodNodeX, floodNodeY,imp.getStackIndex(1,(floodNodeZ-1)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY),(floodNodeZ-1),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX, floodNodeY,imp.getStackIndex(1,(floodNodeZ-1)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX, floodNodeY,imp.getStackIndex(1,(floodNodeZ-1)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ-1;
											floodNodes[index][3] = floodNodeT;
										}                
										if ((floodNodeZ < (slices-1)) 
												&& (imp.getStack().getVoxel(floodNodeX, floodNodeY,imp.getStackIndex(1,(floodNodeZ+1)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY),(floodNodeZ+1),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX, floodNodeY,imp.getStackIndex(1,(floodNodeZ+1)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX, floodNodeY,imp.getStackIndex(1,(floodNodeZ+1)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ+1;
											floodNodes[index][3] = floodNodeT;
										}
										if(processedPxCount%(pxCount100)<pxCount1000){
											progress.setBar(ProgressFactor0*((double)(processedPxCount)/(double)(pxCount)));
											progress.updateBarText("filtering particles ... " + dformat3.format(100.0*(double)(processedPxCount)/(double)(pxCount)) + "%");
										}
									}					
									//Floodfiller
									
									pointCollection.trimToSize();
									
									//Filter Particle
									if(pointCollection.size()<minParticleVolume){
										excludedParticles [t] += pointCollection.size();								
									}else if(onlyLargest){
										if(pointSaveCollection.isEmpty()){
											pointSaveCollection.ensureCapacity(pointCollection.size());
											for(int i = 0; i < pointCollection.size(); i++){
												pointSaveCollection.add(pointCollection.get(i));
											}
										}else if(pointCollection.size()>pointSaveCollection.size()){
											excludedParticles [t] += pointSaveCollection.size();
											pointSaveCollection.clear();
											pointSaveCollection.ensureCapacity(pointCollection.size());
											for(int i = 0; i < pointCollection.size(); i++){
												pointSaveCollection.add(pointCollection.get(i));
											}
										}else{
											excludedParticles [t] += pointCollection.size();
										}
									}else{
										//save points back to image
										for(int i = 0; i < pointCollection.size(); i++){
											pointSaveCollection.add(pointCollection.get(i));
										}
										includedParticles++;										
									}
									//Filter particles
								}		
								newPxCountPF = pxCountPF [t] - excludedParticles [t];
							}
						}						
					}
					
					//write points in saveCollection back to image
					pointSaveCollection.trimToSize();
					for(int i = 0; i < pointSaveCollection.size(); i++){
						imp.getStack().setVoxel(pointSaveCollection.get(i).x, pointSaveCollection.get(i).y,
								imp.getStackIndex(1, pointSaveCollection.get(i).z+1, pointSaveCollection.get(i).t+1)-1,
								pointSaveCollection.get(i).intensity);
					}
					newPxCount -= excludedParticles [t];
					pxCountPF [t] -= excludedParticles [t];
					if(processedPxCount==pxCount){					
						break searchCells;
					}
					pointSaveCollection.clear();
					pointCollection.clear();
				}				
				pxCount = newPxCount;
			}
			System.gc();
			
			if(continueProcessing == false){
				break continueAll;
			}
			
			//Detect Particles			
			ArrayList <ImPoint> wholePointCollection = new ArrayList <ImPoint>(pxCount);
			TimelapseParticle allParticles = new TimelapseParticle();
			if(onlyLargest)	includedParticles = frames;
			ArrayList <TimelapseParticle> particleCollection = new ArrayList <TimelapseParticle>(includedParticles);
			
			if(mergeSelection.equals(mergeOrNotMerge[0])){
				//all points belong to the same object and thus, can be merged immediately
				for(int t = 0; t < frames; t++){
					for(int z = 0; z < slices; z++){
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){
								if(imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1) > 0.0){
									wholePointCollection.add(new ImPoint(x,y,z,t,imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1)));
								}								
							}
						}
					}
				}
				wholePointCollection.trimToSize();
				progress.setBar(ProgressFactor0+ProgressFactor1);
				progress.updateBarText("detecting particles ... " + dformat3.format(100.0) + "%");
				
				allParticles = new TimelapseParticle(wholePointCollection, cal, totalGroupSize, skeletonize, gSigmaXY, gSigmaZ, 
						width, height, slices, frames, false, binarizeBeforeSkl, progress);
				wholePointCollection.clear();
			}else{
				//separate objects over time
				int processedPxCount = 0;
				int pxCount100 = pxCount/100; if (pxCount100==0){pxCount100 = 1;}
				int pxCount1000 = pxCount/1000; if (pxCount1000==0){pxCount1000 = 1;}
				
				ArrayList <ImPoint> pointCollection = new ArrayList <ImPoint>(pxCount);	
				searchCells: for(int t = 0; t < frames; t++){
					for(int z = 0; z < slices; z++){
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){		
								if(imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1) > 0.0){
									pointCollection.clear();
									pointCollection.ensureCapacity(pxCount);
									pointCollection.add(new ImPoint(x,y,z,t,imp.getStack().getVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1)));
									imp.getStack().setVoxel(x,y,imp.getStackIndex(1,z+1,t+1)-1,0.0);
									processedPxCount++;
									
									//Floodfiller					
									floodNodeX = x;
									floodNodeY = y;
									floodNodeZ = z;
									floodNodeT = t;
									index = 0;
									floodNodes = new int[pxCount][4];
									floodNodes[0][0] = floodNodeX;
									floodNodes[0][1] = floodNodeY;
									floodNodes[0][2] = floodNodeZ;
									floodNodes[0][3] = floodNodeT;
									while (index >= 0){
										floodNodeX = floodNodes[index][0];
										floodNodeY = floodNodes[index][1];
										floodNodeZ = floodNodes[index][2];
										floodNodeT = floodNodes[index][3];
										index--;            
										if ((floodNodeX > 0) 
												&& (imp.getStack().getVoxel(floodNodeX-1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX-1),(floodNodeY),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX-1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX-1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX-1;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeX < (width-1)) 
												&& (imp.getStack().getVoxel(floodNodeX+1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX+1),(floodNodeY),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX+1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX+1,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1, 0.0);
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX+1;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeY > 0) 
												&& (imp.getStack().getVoxel(floodNodeX,floodNodeY-1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY-1),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX,floodNodeY-1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX,floodNodeY-1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1, 0.0);
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY-1;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}                
										if ((floodNodeY < (height-1)) 
												&& (imp.getStack().getVoxel(floodNodeX,floodNodeY+1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY+1),(floodNodeZ),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX,floodNodeY+1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX,floodNodeY+1,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT)+1)-1, 0.0);
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY+1;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeZ > 0) 
												&& (imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ-1)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY),(floodNodeZ-1),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ-1)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ-1)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ-1;
											floodNodes[index][3] = floodNodeT;
										}                
										if ((floodNodeZ < (slices-1)) 
												&& (imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ+1)+1,(floodNodeT)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY),(floodNodeZ+1),(floodNodeT),
													imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ+1)+1,(floodNodeT)+1)-1)));
											imp.getStack().setVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ+1)+1,(floodNodeT)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ+1;
											floodNodes[index][3] = floodNodeT;
										}
										if ((floodNodeT > 0) 
												&& (imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT-1)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY),(floodNodeZ),(floodNodeT-1),
													imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT-1)+1)-1)));
											imp.getStack().setVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT-1)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT-1;
										}                
										if ((floodNodeT < (frames-1)) 
												&& (imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT+1)+1)-1) > 0.0)){
											pointCollection.add(new ImPoint((floodNodeX),(floodNodeY),(floodNodeZ),(floodNodeT+1),
													imp.getStack().getVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT+1)+1)-1)));
											imp.getStack().setVoxel(floodNodeX,floodNodeY,imp.getStackIndex(1,(floodNodeZ)+1,(floodNodeT+1)+1)-1, 0.0);
											
											index++;
											processedPxCount++;
											
											floodNodes[index][0] = floodNodeX;
											floodNodes[index][1] = floodNodeY;
											floodNodes[index][2] = floodNodeZ;
											floodNodes[index][3] = floodNodeT+1;
										}
										if(processedPxCount%(pxCount100)<pxCount1000){
											progress.setBar(ProgressFactor0+ProgressFactor1*((double)(processedPxCount)/(double)(pxCount)));
											progress.updateBarText("detecting particles ... " + dformat3.format(100.0*(double)(processedPxCount)/(double)(pxCount)) + "%");
										}
									}					
									//Floodfiller
									
									pointCollection.trimToSize();
									//TimelapseParticle(ArrayList<imPoint> points, Calibration cal, double projectedFrames, boolean skeletonize, 
									//double gaussSigmaXY, double gaussSigmaZ, int orWidth, int orHeight, int orSlices, int orTimes, boolean minimizeImages, 
									//boolean binarizeBeforeSkeletonization, ProgressDialog progress)
									particleCollection.add(new TimelapseParticle(pointCollection, cal, totalGroupSize, skeletonize, 
											gSigmaXY, gSigmaZ, width, height, slices, frames, true, 
											binarizeBeforeSkl, progress));												
								}				
							}
						}
						if(processedPxCount==pxCount){					
							break searchCells;
						}
					}
				}
				particleCollection.trimToSize();
			}
			imp.changes = false;
			imp.close();
			System.gc();
			
			if(continueProcessing == false){
				break continueAll;
			}			
			//Filter particles
			
			progress.setBar(0.8);
					
			/***************************************************************************
			 *#########################################################################* 
			 * 								SAVE RESULTS							   *
			 *#########################################################################* 
			 ************************************************************************* */	
			//Dataname processing
				currentDate = new Date();
				
				String filePrefix = name [task];
				if(name [task].contains(".")){
					filePrefix = name [task].substring(0,name [task].lastIndexOf(".")) + "_M3D";
				}else{
					filePrefix = name [task] + "_M3D";
				}			
				if(saveDate) filePrefix += "_" + NameDateFormatter.format(currentDate);			

				//Create subfolder to save additional files
				String subfolderPrefix = "" + dir [task] + filePrefix + System.getProperty("file.separator") + "M3D";
				try{
					new File(dir [task] + filePrefix).mkdirs();
				}catch(Exception e){
					progress.notifyMessage("failed to create subfolder to save additional plots! Will save plots into origianl folder!",ProgressDialog.NOTIFICATION);
				}
				
				String RPSuffix = "_RP" + dformat0.format(minParticleVolume);
				if(onlyLargest){
					RPSuffix = "_RPL" + dformat0.format(minParticleVolume);
				}
			//Dataname processing
			
			//Generate Legends
				String legendA1 = "";	
				legendA1 += "	" + "Morphological parameters";
				String legendA11 = "";
				for(int s = 0; s < 55; s++){
					legendA11 += "	";
				}
				legendA11 += "	" + "Dynamic parameters - change in comparison to previous time-step";
				String legendA2 = "";
				String legendA21 = "";
				{
					legendA2 += "	" + "time [" + timeUnit + "]";
					legendA2 += "	" + "x center [" + calibrationDimension + "]";
					legendA2 += "	" + "y center [" + calibrationDimension + "]";
					legendA2 += "	" + "z center [" + calibrationDimension + "]";
					legendA2 += "	"; if(semiBinary){ legendA2 += "x center of mass [" + calibrationDimension + "]";}
					legendA2 += "	"; if(semiBinary){ legendA2 += "y center of mass [" + calibrationDimension + "]";}
					legendA2 += "	"; if(semiBinary){ legendA2 += "z center of mass [" + calibrationDimension + "]";}
					legendA2 += "	" + "x span [" + calibrationDimension + "]";
					legendA2 += "	" + "y span [" + calibrationDimension + "]";
					legendA2 += "	" + "z span [" + calibrationDimension + "]";
					legendA2 += "	"; if(semiBinary) legendA2 += "average intensity";
					legendA2 += "	"; if(semiBinary) legendA2 += "minimum intensity";
					legendA2 += "	"; if(semiBinary) legendA2 += "maximum intensity";
					legendA2 += "	"; if(semiBinary) legendA2 += "standard deviation of intensity";
					legendA2 += "	" + "volume [" + calibrationDimension + "^3]";
					legendA2 += "	" + "surface [" + calibrationDimension + "^2]";
					legendA2 += "	" + "ramification index";
					legendA2 += "	" + "spanned volume (convex hull) [" + calibrationDimension + "^3]";
					legendA2 += "	" + "spanned surface (convex hull) [" + calibrationDimension + "^2]";
					legendA2 += "	" + "spanned volume center x (convex hull) [" + calibrationDimension + "]";
					legendA2 += "	" + "spanned volume center y (convex hull) [" + calibrationDimension + "]";
					legendA2 += "	" + "spanned volume center z (convex hull) [" + calibrationDimension + "]";
					legendA2 += "	" + "polarity vector x (binary) [" + calibrationDimension + "]";
					legendA2 += "	" + "polarity vector y (binary) [" + calibrationDimension + "]";
					legendA2 += "	" + "polarity vector z (binary) [" + calibrationDimension + "]";
					legendA2 += "	" + "polarity vector length (binary) [" + calibrationDimension + "]";
					legendA2 += "	" + "polarity index (binary)";
					
					legendA2 += "	"; if(semiBinary){ legendA2 += "polarity vector x [" + calibrationDimension + "]";}
					legendA2 += "	"; if(semiBinary){ legendA2 += "polarity vector y [" + calibrationDimension + "]";}
					legendA2 += "	"; if(semiBinary){ legendA2 += "polarity vector z [" + calibrationDimension + "]";}
					legendA2 += "	"; if(semiBinary){ legendA2 += "polarity vector length [" + calibrationDimension + "]";}
					legendA2 += "	"; if(semiBinary){ legendA2 += "polarity index";}
				
					legendA2 += "	"; if(skeletonize){ legendA2 += "ID of largest skeleton (skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# found skls";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# branches (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# junctions (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# tips (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# triple points (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# quadruple points (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# junction voxels (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# slab voxels (largest skl)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "tree length (largest skl) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "average branch length (largest skl) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "maximum branch length (largest skl) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "shortest path (largest skl) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# branches (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# junctions (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# tips (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# triple points (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# quadruple points (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# junction voxels (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "# slab voxels (all skls)";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "tree length (all skls) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "average branch length (all skls) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "maximum branch length (all skls) [" + calibrationDimension + "]";}
					legendA2 += "	"; if(skeletonize){ legendA2 += "shortest path (all skls) [" + calibrationDimension + "]";}
								
					legendA21 += "	" + "moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]";
					legendA21 += "	"; if(semiBinary){ legendA21 += "moving vector length [" + calibrationDimension + "/" + timeUnit + "]";}
					legendA21 += "	" + "extended volume [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendA21 += "	" + "retracted volume [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendA21 += "	" + "shape dynamics (extended + retracted volume) [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendA21 += "	" + "measured volume difference [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendA21 += "	" + "measured ramification index difference [1/" + timeUnit + "]";
					legendA21 += "	" + "# extensions [1/" + timeUnit + "]";
					legendA21 += "	" + "# retractions [1/" + timeUnit + "]";
					legendA21 += "	" + "# extensions (> 1 voxel) [1/" + timeUnit + "]";
					legendA21 += "	" + "# retractions (> 1 voxel) [1/" + timeUnit + "]";
				}
				String legendB1 = "";
				{
					legendB1 += "		" + "Long-term scanning behaviour";
					for(int s = 0; s < 5; s++){
						legendB1 += "	";
					}
					legendB1 += "	" + "Long-term directionality of movement (binary)";
					legendB1 += "		";
					legendB1 += "	"; if(semiBinary){ legendB1 += "Long-term directionality of movement";}
					legendB1 += "		";
					legendB1 += "		" + "Averages (av) morphological parameters (" + dformat0.format(totalGroupSize) +" time-steps/group)";
					for(int s = 0; s < 53; s++){
						legendB1 += "	";
					}
					legendB1 += "	" + "Averages (av) of dynamic parameters (" + dformat0.format(totalGroupSize) +" time-steps/group)";
				}
				String legendB2 = "";
				{
					double groupTime = (totalGroupSize-1)*timePerFrame;
					String groupTimeString = dformat3.format(groupTime) + " " + timeUnit;;
					if(groupTime-(double)((int)groupTime) == 0.0){
						groupTimeString = dformat0.format(groupTime) + " " + timeUnit;
					}
					
					legendB2 += "	" + "grouped time";
					legendB2 += "	" + "projected volume (= 'scanned volume') [" + calibrationDimension + "^3/" + groupTimeString + "]";
					legendB2 += "	" + "static volume [" + calibrationDimension + "^3/" + groupTimeString + "]";
					legendB2 += "	" + "dynamic fraction of projected volume (= 'scanning activity')";
					legendB2 += "	" + "projected spanned volume (convex hull) [" + calibrationDimension + "^3/" + groupTimeString + "]";
					legendB2 += "	" + "static spanned volume (convex hull) [" + calibrationDimension + "^3/" + groupTimeString + "]";
					legendB2 += "	" + "dynamic fraction of spanned volume (convex hull)";
					
					legendB2 += "	" + "accumulated distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
					legendB2 += "	" + "euclidean distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
					legendB2 += "	" + "directionality index = eucledian/accumulated distance (binary)";
			
					legendB2 += "	"; if(semiBinary){legendB2 += "accumulated distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]";}
					legendB2 += "	"; if(semiBinary){legendB2 += "euclidean distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]";}
					legendB2 += "	"; if(semiBinary){legendB2 += "directionality index = eucledian/accumulated distance (center of mass)";}
			
					legendB2 += "	";
					//averages
					legendB2 += "	" + "av x center [" + calibrationDimension + "]";
					legendB2 += "	" + "av y center [" + calibrationDimension + "]";
					legendB2 += "	" + "av z center [" + calibrationDimension + "]";
					legendB2 += "	"; if(semiBinary){ legendB2 += "av x center of mass [" + calibrationDimension + "]";}
					legendB2 += "	"; if(semiBinary){ legendB2 += "av y center of mass [" + calibrationDimension + "]";}
					legendB2 += "	"; if(semiBinary){ legendB2 += "av z center of mass [" + calibrationDimension + "]";}
					legendB2 += "	" + "av x span [" + calibrationDimension + "]";
					legendB2 += "	" + "av y span [" + calibrationDimension + "]";
					legendB2 += "	" + "av z span [" + calibrationDimension + "]";
					legendB2 += "	"; if(semiBinary) legendB2 += "av average intensity";
					legendB2 += "	"; if(semiBinary) legendB2 += "av minimum intensity";
					legendB2 += "	"; if(semiBinary) legendB2 += "av maximum intensity";
					legendB2 += "	"; if(semiBinary) legendB2 += "av standard deviation of intensity";
					legendB2 += "	" + "av volume [" + calibrationDimension + "^3]";
					legendB2 += "	" + "av surface [" + calibrationDimension + "]^2";
					legendB2 += "	" + "av ramification index";
					legendB2 += "	" + "av spanned volume (convex hull) [" + calibrationDimension + "^3]";
					legendB2 += "	" + "av spanned surface (convex hull) [" + calibrationDimension + "]";
					legendB2 += "	" + "av spanned volume center x (convex hull) [" + calibrationDimension + "]";
					legendB2 += "	" + "av spanned volume center y (convex hull) [" + calibrationDimension + "]";
					legendB2 += "	" + "av spanned volume center z (convex hull) [" + calibrationDimension + "]";
					legendB2 += "	" + "av polarity vector x (binary) [" + calibrationDimension + "]";
					legendB2 += "	" + "av polarity vector y (binary) [" + calibrationDimension + "]";
					legendB2 += "	" + "av polarity vector z (binary) [" + calibrationDimension + "]";
					legendB2 += "	" + "av polarity vector length (binary) [" + calibrationDimension + "]";
					legendB2 += "	" + "av polarity index (binary)";
					
					legendB2 += "	"; if(semiBinary){legendB2 += "av polarity vector x [" + calibrationDimension + "]";}
					legendB2 += "	"; if(semiBinary){legendB2 += "av polarity vector y [" + calibrationDimension + "]";}
					legendB2 += "	"; if(semiBinary){legendB2 += "av polarity vector z [" + calibrationDimension + "]";}
					legendB2 += "	"; if(semiBinary){legendB2 += "av polarity vector length [" + calibrationDimension + "]";}
					legendB2 += "	"; if(semiBinary){legendB2 += "av polarity index";}
				
					legendB2 += "	"; if(skeletonize){legendB2 += "av # found skls";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # branches (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # junctions (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # tips (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # triple points (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # quadruple points (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # junction voxels (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # slab voxels (largest skl)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av tree length (largest skl) [" + calibrationDimension + "]";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av average branch length (largest skl) [" + calibrationDimension + "]";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av maximum branch length (largest skl) [" + calibrationDimension + "]";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av shortest path (largest skl) [" + calibrationDimension + "]";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # branches (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # junctions (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # tips (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # triple points (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # quadruple points (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # junction voxels (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av # slab voxels (all skls)";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av tree length (all skls) [" + calibrationDimension + "]";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av average branch length (all skls) [" + calibrationDimension + "]";}
					legendB2 += "	"; if(skeletonize){legendB2 += "av maximum branch length (all skls) [" + calibrationDimension + "]";}	
					legendB2 += "	"; if(skeletonize){legendB2 += "av shortest path (all skls) [" + calibrationDimension + "]";}
					
					legendB2 += "	" + "av moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]";
					legendB2 += "	"; if(semiBinary){legendB2+= "av moving vector length [" + calibrationDimension + "/" + timeUnit + "]";}
					legendB2 += "	" + "av extended volume [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendB2 += "	" + "av retracted volume [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendB2 += "	" + "av shape dynamics (extended + retracted volume) [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendB2 += "	" + "av measured volume difference [" + calibrationDimension + "^3/" + timeUnit + "]";
					legendB2 += "	" + "av measured ramification index difference [1/" + timeUnit + "]";
					legendB2 += "	" + "av # extensions [1/" + timeUnit + "]";
					legendB2 += "	" + "av # retractions [1/" + timeUnit + "]";
					legendB2 += "	" + "av # extensions (> 1 voxel) [1/" + timeUnit + "]";
					legendB2 += "	" + "av # retractions (> 1 voxel) [1/" + timeUnit + "]";
				}
			//Generate legends
			
			//Save results text files
				TextPanel tw1 =new TextPanel("Results");
				tw1.append("Saving date:	" + FullDateFormatter.format(currentDate) + "	Analysis started:	" + FullDateFormatter.format(startDate));
				tw1.append("Image");
				tw1.append("	name:	" + name [task]);
				if(semiBinary){
					tw1.append("	image contains intensity information");
				}else{
					tw1.append("");
				}
				tw1.append("Settings:");
				tw1.append("	Width [px]:	" + dformat0.format(width) + "	Height [px]:	" + dformat0.format(height) + "	# z-slices:	" + dformat0.format(slices) + "	# time steps:	" + dformat0.format(frames));
				tw1.append("	Length calibration [" + calibrationDimension + "/px]:	" + dformat6.format(calibration));
				tw1.append("	Voxel depth [" + calibrationDimension + "/px]:	" + dformat6.format(voxelDepth));
				tw1.append("	Time interval [" + timeUnit + "]:	" + dformat6.format(timePerFrame));
				tw1.append("	Frames per second:	" + dformat6.format(fps));
				tw1.append("	# time-steps grouped for long-term analysis:	" + dformat0.format(totalGroupSize));
				tw1.append("	Particle filtering and tracking:");
				tw1.append("		Minimum accepted particle volume [voxel]:	" + dformat0.format(minParticleVolume));
				if(onlyLargest){
					tw1.append("		Remove all particles but the largest one.");
				}else{
					tw1.append("");
				}
				if(mergeSelection.equals(mergeOrNotMerge[0])){
					tw1.append("		All particles merged into one object for analysis.");
				}else{
					tw1.append("		Particles were tracked by overlap and analyzed seperately.");
				}
				if(skeletonize){
					String sklAppendTxt = "	Skeleton was analyzed - gauss filter:	Sigma xy: " + dformat3.format(gSigmaXY) + "	Sigma z: " + dformat3.format(gSigmaZ);
					if(binarizeBeforeSkl&&semiBinary){
						sklAppendTxt += "	" + "Skeleton was determined in a binary version of the image.";
					}
					sklAppendTxt += "	Skeleton results are generated using the ImageJ plugins 'Skeletonize3D' and 'AnalyzeSkeleton'"
					+ " by: Ignacio Arganda-Carreras, Rodrigo Fernandez-Gonzalez, Arrate Munoz-Barrutia, Carlos Ortiz-De-Solorzano,"
					+ " '3D reconstruction of histological sections: Application to mammary gland tissue', Microscopy Research and Technique,"
					+ " Volume 73, Issue 11, pages 1019-1029, October 2010.";
					tw1.append(sklAppendTxt);
				}else{
					tw1.append("");
				}
				tw1.append("");
				
				// Results for all particles merged into one object
				int nrOfParticles = 1;
				if(mergeSelection.equals(mergeOrNotMerge[1])){
					nrOfParticles = particleCollection.size();
				}
				for(int p = 0; p < nrOfParticles; p++){
					try{
						TimelapseParticle particle = new TimelapseParticle();
						if(mergeSelection.equals(mergeOrNotMerge[0])){
							particle = allParticles;
							tw1.append("Short-term results (by time-step)");
						}else if(mergeSelection.equals(mergeOrNotMerge[1])){
							particle = particleCollection.get(p);
							tw1.append("Short-term results (by time-step) - particle nr " + dformat0.format(p+1));
						}
						
						if(particle.initialized){
							if(particle.times>1){
								tw1.append(legendA1+legendA11);
								tw1.append(legendA2+legendA21);
							}else{
								tw1.append(legendA1);
								tw1.append(legendA2);
							}				
							for(int t = 0; t < particle.times; t++){
								String appendTxt = "";
								appendTxt += "	" + dformat6.format((particle.tMin+t)*timePerFrame);	//	"time [" + timeUnit + "]";
								appendTxt += "	" + dformat6.format(particle.xC [t] + xCorr*calibration);	//	"x center [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.yC [t] + yCorr*calibration);
								appendTxt += "	" + dformat6.format(particle.zC [t] + zCorr*voxelDepth);
								appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.xCOM [t] + xCorr*calibration);}	//	"x center of mass [" + calibrationDimension + "]";
								appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.yCOM [t] + yCorr*calibration);}
								appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.zCOM [t] + zCorr*voxelDepth);}
								appendTxt += "	" + dformat6.format(particle.xSpan [t]);	//	"x span [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.ySpan [t]);
								appendTxt += "	" + dformat6.format(particle.zSpan [t]);
								appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.averageIntensity [t]);
								appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.minimumIntensity [t]);
								appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.maximumIntensity [t]);
								appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.sdIntensity [t]);
								appendTxt += "	" + dformat6.format(particle.volume [t]);	//	"volume [" + calibrationDimension + "^2]";
								appendTxt += "	" + dformat6.format(particle.surface [t]);	//	"surface [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.RI [t]);	//	"ramification index";
								appendTxt += "	" + dformat6.format(particle.convexHullVolume [t]);	//	"spanned volume (convex hull) [" + calibrationDimension + "^2]";
								appendTxt += "	" + dformat6.format(particle.convexHullSurface [t]);	//	"spanned surface (convex hull) [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.convexHullxC [t] + xCorr*calibration);	//	"spanned volume center x (convex hull) [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.convexHullyC [t] + yCorr*calibration);
								appendTxt += "	" + dformat6.format(particle.convexHullzC [t] + zCorr*voxelDepth);
								appendTxt += "	" + dformat6.format(particle.xPolarityVectorBIN [t]);	//	"polarity vector x (binary) [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.yPolarityVectorBIN [t]);
								appendTxt += "	" + dformat6.format(particle.zPolarityVectorBIN [t]);
								appendTxt += "	" + dformat6.format(particle.polarityVectorLengthBIN [t]);	//	"polarity vector length (binary) [" + calibrationDimension + "]";
								appendTxt += "	" + dformat6.format(particle.polarityIndexBIN [t]);	//	"polarity index (binary)";
								
								
								appendTxt += "	"; if(semiBinary){appendTxt += dformat6.format(particle.xPolarityVectorSemiBIN [t]);}							
								appendTxt += "	"; if(semiBinary){appendTxt += dformat6.format(particle.yPolarityVectorSemiBIN [t]);}
								appendTxt += "	"; if(semiBinary){appendTxt += dformat6.format(particle.zPolarityVectorSemiBIN [t]);}
								appendTxt += "	"; if(semiBinary){appendTxt += dformat6.format(particle.polarityVectorLengthSemiBIN [t]);}	
								appendTxt += "	"; if(semiBinary){appendTxt += dformat6.format(particle.polarityIndexSemiBIN [t]);}	
								
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.IDofLargest [t]+1);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.foundSkl [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.branches [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.junctions [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.tips [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.triplePs [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.quadruplePs [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.junctionVx [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.slabVx [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.treeLength [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.avBranchLength [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.maxBranchLength [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.largestShortestPath [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.branchesOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.junctionsOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.tipsOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.triplePsOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.quadruplePsOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.junctionVxOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat0.format(particle.slabVxOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.treeLengthOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.avBranchLengthOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.maxBranchLengthOfAll [t]);}
								appendTxt += "	"; if(skeletonize){appendTxt += dformat6.format(particle.largestShortestPathOfAll [t]);}
							
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.movingVectorLengthBIN [t]/timePerFrame);}
								appendTxt += "	"; if(t!=0 && semiBinary){ appendTxt += dformat6.format(particle.movingVectorLengthSemiBIN [t]/timePerFrame);}	
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.occupVolume [t]/timePerFrame);}
									//	"extended volume [" + calibrationDimension + "^2/" + timeUnit + "]";
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.lostVolume [t]/timePerFrame);}
									//	"retracted volume [" + calibrationDimension + "^2/" + timeUnit + "]";
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.motility [t]/timePerFrame);}
									//	"shape dynamics (extended + retracted volume) [" + calibrationDimension + "^2/" + timeUnit + "]";
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.deltaVolume [t]/timePerFrame);}
									//	"measured volume difference [" + calibrationDimension + "^2/" + timeUnit + "]";
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.deltaRI [t]/timePerFrame);}
									//	"measured ramification index difference [1/" + timeUnit + "]";
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.nrOfExtensions [t]/timePerFrame);}
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.nrOfRetractions [t]/timePerFrame);}
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.nrOfExtensionsMoreThan1Vx [t]/timePerFrame);}	
									//	"# extensions (> 1 voxel) [1/" + timeUnit + "]";
								appendTxt += "	"; if(t!=0){ appendTxt += dformat6.format(particle.nrOfRetractionsMoreThan1Vx [t]/timePerFrame);}
									//	"# retractions (> 1 voxel) [1/" + timeUnit + "]";
								tw1.append(appendTxt);
							}
							tw1.append("");
							if(particle.times>=totalGroupSize){
								if(mergeSelection.equals(mergeOrNotMerge[0])){
									tw1.append("Long-term results (by time-step group)");
								}else if(mergeSelection.equals(mergeOrNotMerge[1])){
									tw1.append("Long-term results (by time-step group) - particle nr " + dformat0.format(p+1));
								}
								tw1.append(legendB1);
								tw1.append(legendB2);
								for(int group = 0; group < particle.projectedTimes; group++){
									String appendTxt = "";
									double groupTime0 = group*totalGroupSize*timePerFrame;
									String groupTimeText0 = dformat3.format(groupTime0);
									if(groupTime0 - (double)((int)groupTime0)==0.0){
										groupTimeText0 = dformat0.format(groupTime0);
									}
									double groupTime1 = ((group+1)*totalGroupSize-1)*timePerFrame;
									String groupTimeText1 = dformat3.format(groupTime1);
									if(groupTime1 - (double)((int)groupTime1)==0.0){
										groupTimeText1 = dformat0.format(groupTime1);
									}
									appendTxt += "	" + groupTimeText0 + " - " + groupTimeText1 + " " + timeUnit;
									appendTxt += "	" + dformat6.format(particle.projectedVolume [group]);	//	"projected volume (= 'scanned volume') [" + calibrationDimension + "^2/" + groupTimeString + "]";
									appendTxt += "	" + dformat6.format(particle.projectedStaticVolume [group]);	//	"static volume [" + calibrationDimension + "^2/" + groupTimeString + "]";
									appendTxt += "	" + dformat6.format(particle.projectedDynamicFraction [group]);	//	"dynamic fraction of projected volume (= 'scanning activity')";
									appendTxt += "	" + dformat6.format(particle.projectedConvexHullVolume [group]);	//	"projected spanned volume (convex hull) [" + calibrationDimension + "^2/" + groupTimeString + "]";
									appendTxt += "	" + dformat6.format(particle.projectedConvexHullStaticVolume [group]);	//	"static spanned volume (convex hull) [" + calibrationDimension + "^2/" + groupTimeString + "]";
									appendTxt += "	" + dformat6.format(particle.projectedConvexHullDynamicFraction [group]);	//	"dynamic fraction of spanned volume (convex hull)";
									
									appendTxt += "	" + dformat6.format(particle.accumulatedDistanceBIN [group]);	//	"accumulated distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
									appendTxt += "	" + dformat6.format(particle.euclideanDistanceBIN [group]);	//	"euclidean distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
									appendTxt += "	" + dformat6.format(particle.directionalityIndexBIN [group]);	//	"directionality index = eucledian/accumulated distance (binary)";
								
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.accumulatedDistanceSemiBIN [group]);}	//	"accumulated distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.euclideanDistanceSemiBIN [group]);}	//	"euclidean distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.directionalityIndexSemiBIN [group]);}	//	"directionality index = eucledian/accumulated distance (center of mass)";
								
									appendTxt += "	";	
									
									//averages per Group
									appendTxt += "	" + dformat6.format(particle.avXC [group]+xCorr*calibration);	//	"x center [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avYC [group]+yCorr*calibration);
									appendTxt += "	" + dformat6.format(particle.avZC [group]+yCorr*voxelDepth);
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avXCOM [group]+xCorr*calibration);}	//	"x center of mass [" + calibrationDimension + "]";
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avYCOM [group]+yCorr*calibration);}
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avZCOM [group]+yCorr*voxelDepth);}
									appendTxt += "	" + dformat6.format(particle.avXSpan [group]);	//	"x span [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avYSpan [group]);
									appendTxt += "	" + dformat6.format(particle.avZSpan [group]);
									appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.avAverageIntensity [group]);
									appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.avMinimumIntensity [group]);
									appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.avMaximumIntensity [group]);
									appendTxt += "	"; if(semiBinary) appendTxt += dformat6.format(particle.avSdIntensity [group]);
									appendTxt += "	" + dformat6.format(particle.avVolume [group]);	//	"volume [" + calibrationDimension + "^2]";
									appendTxt += "	" + dformat6.format(particle.avSurface [group]);	//	"surface [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avRI [group]);	//	"ramification index";
									appendTxt += "	" + dformat6.format(particle.avConvexHullVolume [group]);	//	"spanned volume (convex hull) [" + calibrationDimension + "^2]";
									appendTxt += "	" + dformat6.format(particle.avConvexHullSurface [group]);	//	"spanned surface (convex hull) [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avConvexHullxC [group]+xCorr*calibration);	//	"spanned volume center x (convex hull) [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avConvexHullyC [group]+yCorr*calibration);
									appendTxt += "	" + dformat6.format(particle.avConvexHullzC [group]+zCorr*voxelDepth);
									appendTxt += "	" + dformat6.format(particle.avXPolarityVectorBIN [group]);	//	"polarity vector x (binary) [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avYPolarityVectorBIN [group]);
									appendTxt += "	" + dformat6.format(particle.avZPolarityVectorBIN [group]);
									appendTxt += "	" + dformat6.format(particle.avPolarityVectorLengthBIN [group]);	//	"polarity vector length (binary) [" + calibrationDimension + "]";
									appendTxt += "	" + dformat6.format(particle.avPolarityIndexBIN [group]);	//	"polarity index (binary)";
									
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avXPolarityVectorSemiBIN [group]);}	//	"polarity vector x [" + calibrationDimension + "]";
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avYPolarityVectorSemiBIN [group]);}
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avZPolarityVectorSemiBIN [group]);}
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avPolarityVectorLengthSemiBIN [group]);}	//	"polarity vector length [" + calibrationDimension + "]";
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avPolarityIndexSemiBIN [group]);}	//	"polarity index";
								
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avFoundSkl [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avBranches [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avJunctions [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avTips [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avTriplePs [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avQuadruplePs [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avJunctionVx [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avSlabVx [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avTreeLength [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avAvBranchLength [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avMaxBranchLength [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avLargestShortestPath [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avBranchesOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avJunctionsOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avTipsOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avTriplePsOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avQuadruplePsOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avJunctionVxOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avSlabVxOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avTreeLengthOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avAvBranchLengthOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avMaxBranchLengthOfAll [group]);}
									appendTxt += "	"; if(skeletonize){ appendTxt += dformat6.format(particle.avLargestShortestPathOfAll [group]);}
								
									appendTxt += "	" + dformat6.format(particle.avMovingVectorLengthBIN [group]/timePerFrame);	//	"moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]";
									appendTxt += "	"; if(semiBinary){ appendTxt += dformat6.format(particle.avMovingVectorLengthSemiBIN [group]/timePerFrame);}	//	"moving vector length [" + calibrationDimension + "/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avOccupVolume [group]/timePerFrame);					//	"extended volume [" + calibrationDimension + "^2/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avLostVolume [group]/timePerFrame);					//	"retracted volume [" + calibrationDimension + "^2/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avMotility [group]/timePerFrame);					//	"shape dynamics (extended + retracted volume) [" + calibrationDimension + "^2/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avDeltaVolume [group]/timePerFrame);					//	"measured volume difference [" + calibrationDimension + "^2/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avDeltaRI [group]/timePerFrame);						//	"measured ramification index difference [1/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avNrOfExtensions [group]/timePerFrame);				//	"# extensions [1/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avNrOfRetractions [group]/timePerFrame);				//	"# retractions [1/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avNrOfExtensionsMoreThan1Px [group]/timePerFrame);	//	"# extensions (> 1 voxel) [1/" + timeUnit + "]";
									appendTxt += "	" + dformat6.format(particle.avNrOfRetractionsMoreThan1Px [group]/timePerFrame);	//	"# retractions (> 1 voxel) [1/" + timeUnit + "]";
									tw1.append(appendTxt);
								}
							}
						}
					}catch(Exception e){
						progress.notifyMessage("Task " + task + "/" + tasks + ": error - could not correctly write results...",ProgressDialog.ERROR);
					}
					tw1.append("");
				}			
				tw1.append("Datafile was generated by '"+PLUGINNAME+"',"
						+ " \u00a9 2014 - " + yearOnly.format(new Date()) + " Jan Niklas Hansen (jan.hansen@uni-bonn.de).");
				tw1.append("The plugin '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
						+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
						+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
				tw1.append("Plugin version:	"+PLUGINVERSION);			
				tw1.saveAs(dir[task] + filePrefix + ".txt");
				
				//save one row results text files
					//save legend
					saveLegendST(frames, (subfolderPrefix + "l1L.txt"));
					if(frames>=totalGroupSize){
						saveLegendLT(frames, (subfolderPrefix + "l2L.txt"));
					}								
				if(mergeSelection.equals(mergeOrNotMerge[0])){
					//save results
					ArrayList<TimelapseParticle> onePartLL = new ArrayList<TimelapseParticle>(1);
					onePartLL.add(allParticles);
					saveOneRowResultsST(onePartLL, frames, xCorr, yCorr, zCorr, dir [task], name [task], (dir[task] + filePrefix + "l1.txt"));
					if(frames>=totalGroupSize){
						saveOneRowResultsLT(onePartLL, frames, xCorr, yCorr, zCorr, dir [task], name [task], (dir[task] + filePrefix + "l2.txt"));
					}				
				}else if(mergeSelection.equals(mergeOrNotMerge[1])){
					//save results
					saveOneRowResultsST(particleCollection, frames, xCorr, yCorr, zCorr, dir [task], name [task], (dir[task] + filePrefix + "l1.txt"));
					if(frames>=totalGroupSize){
						saveOneRowResultsLT(particleCollection, frames, xCorr, yCorr, zCorr, dir [task], name [task], (dir[task] + filePrefix + "l2.txt"));
					}				
				}	
				
				//Save Images	
				v3D = new Visualizer3D(imp, 3.0f);
				v3D.setAngle(10.0f, -10.0f, 0.0f);
				
				progress.updateBarText("generating visualizations...");
				if(mergeSelection.equals(mergeOrNotMerge[0])){				
					IJ.saveAs(allParticles.particleImp, "tif", subfolderPrefix + RPSuffix + ".tif");
					IJ.saveAs(allParticles.convexHullImp, "tif", subfolderPrefix + "_H.tif");
					if(skeletonize){
						IJ.saveAs(allParticles.skeletonImp, "tif", subfolderPrefix + "_Skl.tif");
					}
					try{
						allParticles.save3DVisualizations(subfolderPrefix, RPSuffix, v3D);
					}catch(Exception e){
						String out = "";
						for(int err = 0; err < e.getStackTrace().length; err++){
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": No 3D visualization generated - an error occured: " + out,ProgressDialog.NOTIFICATION);						
					}					
					allParticles.closeImps();
				}else if(mergeSelection.equals(mergeOrNotMerge[1])){
					for(int i = 0; i < particleCollection.size(); i++){
						IJ.saveAs(particleCollection.get(i).particleImp, "tif", subfolderPrefix + "_P" + dformat0.format(i+1) + RPSuffix + ".tif");
						IJ.saveAs(particleCollection.get(i).convexHullImp, "tif", subfolderPrefix + "_P" + dformat0.format(i+1) + "_H.tif");
						if(skeletonize){
							IJ.saveAs(particleCollection.get(i).skeletonImp, "tif", subfolderPrefix + "_P" + dformat0.format(i+1) + "_Skl.tif");
						}
						try{
							particleCollection.get(i).save3DVisualizations(subfolderPrefix + "_P" + dformat0.format(i+1), RPSuffix, v3D);
						}catch(Exception e){
							String out = "";
							for(int err = 0; err < e.getStackTrace().length; err++){
								out += " \n " + e.getStackTrace()[err].toString();
							}
							progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": An error occured when generating a 3D visualization - the analysis and all other output files than the missing 3D file are however unaffected and valid!"
									+ " Debuggin information: " + out + "",ProgressDialog.NOTIFICATION);		
						}	
						particleCollection.get(i).closeImps();
					}				
				}
				progress.setBar(0.9);
				
			//Save Images
				
			//Save results text files
				
			//Update Multi-Task-Manager
			progress.setBar(1.0);
			progress.moveTask(task);			
			break running;			
		}	
		}
		allTasksDone = true;
		break continueAll;
		}
	}

	/**
	 * saves a legend file for the one-line files that save short-term parameters
	 * */
	void saveLegendST(int frames, String savePath){
		TextPanel tw =new TextPanel("Legend P1");
		
		String legendTw2 = "		particle nr";
		String legendTw2l2 = "			";
		String spacerTabs = "";
		String spacerTabs2l2 = "";
		for(int i = 0; i < frames; i++){
			spacerTabs += "	";
			spacerTabs2l2 += dformat6.format((double)(i)*timePerFrame) + "	";
		}
		spacerTabs2l2 += "	";
		
		legendTw2 += "	" + "x center [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "y center [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "z center [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "x center of mass [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "y center of mass [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "z center of mass [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "x span [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "y span [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "z span [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "average intensity" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "minimum intensity" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "maximum intensity" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "standard deviation of intensity" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "volume [" + calibrationDimension + "^3]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "surface [" + calibrationDimension + "]^2" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "ramification index" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "spanned volume (convex hull) [" + calibrationDimension + "^3]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "spanned surface (convex hull) [" + calibrationDimension + "]^2" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "spanned volume center x (convex hull) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "spanned volume center y (convex hull) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "spanned volume center z (convex hull) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector x (binary) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector y (binary) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector z (binary) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector length (binary) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity index (binary)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector x [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector y [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector z [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity vector length [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "polarity index" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		
		legendTw2 += "	" + "ID of largest skeleton (skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# found skls" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# branches (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# junctions (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# tips (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# triple points (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# quadruple points (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# junction voxels (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# slab voxels (largest skl)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "tree length (largest skl) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "average branch length (largest skl) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "maximum branch length (largest skl) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "shortest path (largest skl) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# branches (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# junctions (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# tips (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# triple points (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# quadruple points (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# junction voxels (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "# slab voxels (all skls)" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "tree length (all skls) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "average branch length (all skls) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "maximum branch length (all skls) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		legendTw2 += "	" + "shortest path (all skls) [" + calibrationDimension + "]" + spacerTabs;	legendTw2l2 +=spacerTabs2l2;
		
		if(frames>1){
			String legendTw21 = "";
			String legendTw21l2 = "";
			legendTw21 += "	" + "moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "moving vector length [" + calibrationDimension + "/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "extended volume [" + calibrationDimension + "^3/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "retracted volume [" + calibrationDimension + "^3/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "shape dynamics (extended + retracted volume) [" + calibrationDimension + "^3/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "measured volume difference [" + calibrationDimension + "^3/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "measured ramification index difference [1/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "# extensions [1/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "# retractions [1/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "# extensions (> 1 voxel) [1/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			legendTw21 += "	" + "# retractions (> 1 voxel) [1/" + timeUnit + "]" + spacerTabs;	legendTw21l2 +=spacerTabs2l2;
			
			tw.append(legendTw2+legendTw21
					+ "	Skeleton results are generated using the ImageJ plugins 'Skeletonize3D' and 'AnalyzeSkeleton'"
					+ " by: Ignacio Arganda-Carreras, Rodrigo Fernandez-Gonzalez, Arrate Munoz-Barrutia, Carlos Ortiz-De-Solorzano,"
					+ " '3D reconstruction of histological sections: Application to mammary gland tissue', Microscopy Research and Technique,"
					+ " Volume 73, Issue 11, pages 1019-1029, October 2010.");
			tw.append(legendTw2l2+legendTw21l2);
		}else{
			tw.append(legendTw2
					+ "	Skeleton results are generated using the ImageJ plugins 'Skeletonize3D' and 'AnalyzeSkeleton'"
					+ " by: Ignacio Arganda-Carreras, Rodrigo Fernandez-Gonzalez, Arrate Munoz-Barrutia, Carlos Ortiz-De-Solorzano,"
					+ " '3D reconstruction of histological sections: Application to mammary gland tissue', Microscopy Research and Technique,"
					+ " Volume 73, Issue 11, pages 1019-1029, October 2010.");
			tw.append(legendTw2l2);
		}	
		tw.saveAs(savePath);	
	}

	/**
	 * saves a legend file for the one-line files that save long-term parameters
	 * */
	void saveLegendLT(int frames, String savePath){
		int nrOfGroups = (int)((double)frames/(double)totalGroupSize);
		String spacer = "";
		for(int g = 0; g < nrOfGroups; g++)spacer += "	";
		
		TextPanel tp = new TextPanel ("Long-term parameters legend");
		String legendLTheader = "	";
		legendLTheader += "		" + "Long-term scanning behaviour" + spacer;
		for(int s = 0; s < 5; s++){
			legendLTheader += "	" + spacer;
		}
		legendLTheader += "	" + "Long-term directionality of movement (binary)" + spacer;
		legendLTheader += "		" + spacer + spacer;
		legendLTheader += "	" + "Long-term directionality of movement" + spacer;
		legendLTheader += "		" + spacer + spacer;
		
		legendLTheader += "	" + "Averages (av) morphological parameters (" + dformat0.format(totalGroupSize) +" time-steps/group)" + spacer;	
		
		for(int s = 0; s < 53; s++){
			if(s==26){
				legendLTheader += "	Skeleton results are generated using the ImageJ plugins 'Skeletonize3D' and 'AnalyzeSkeleton'"
						+ " by: Ignacio Arganda-Carreras, Rodrigo Fernandez-Gonzalez, Arrate Munoz-Barrutia, Carlos Ortiz-De-Solorzano,"
						+ " '3D reconstruction of histological sections: Application to mammary gland tissue', Microscopy Research and Technique,"
						+ " Volume 73, Issue 11, pages 1019-1029, October 2010." + spacer;
			}else{
				legendLTheader += "	" + spacer;
			}
			
		}
		legendLTheader += "	" + "Averages (av) of dynamic parameters (" + dformat0.format(totalGroupSize) +" time-steps/group)" + spacer;
		tp.append(legendLTheader);
		
		//************
		
		double groupTime = totalGroupSize*timePerFrame;
		String groupTimeString = dformat3.format(groupTime) + " " + timeUnit;;
		if(groupTime-(double)((int)groupTime) == 0.0){
			groupTimeString = dformat0.format(groupTime) + " " + timeUnit;
		}
		
		String legendLT = "		particle nr";
		String legendLTtime = "		";
		String timeSpacer = "";
		for(int i = 0; i < nrOfGroups; i++){
			if(i == 0){
				timeSpacer += "	" + "1st " + groupTimeString;
			}else if (i == 1){
				timeSpacer += "	" + "2nd " + groupTimeString;
			}else if (i == 2){
				timeSpacer += "	" + "3rd " + groupTimeString;
			}else{
				timeSpacer += "	" + dformat0.format(i+1) + "th " + groupTimeString;
			}	
		}
		timeSpacer += "	";
		
		legendLT += "	" + "projected volume (= 'scanned volume') [" + calibrationDimension + "^3/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "static volume [" + calibrationDimension + "^3/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "dynamic fraction of projected volume (= 'scanning activity')" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "projected spanned volume (convex hull) [" + calibrationDimension + "^3/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "static spanned volume (convex hull) [" + calibrationDimension + "^3/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "dynamic fraction of spanned volume (convex hull) " + spacer;	legendLTtime += timeSpacer;
		
		legendLT += "	" + "accumulated distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "euclidean distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "directionality index = eucledian/accumulated distance (binary)" + spacer;	legendLTtime += timeSpacer;
		
		legendLT += "	" + "accumulated distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "euclidean distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "directionality index = eucledian/accumulated distance (center of mass)" + spacer;	legendLTtime += timeSpacer;
		
		//averages
		legendLT += "	" + "av x center [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av y center [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av z center [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av x center of mass [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av y center of mass [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av z center of mass [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av x span [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av y span [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av z span [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av average intensity" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av minimum intensity" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av maximum intensity" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av standard deviation of intensity" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av volume [" + calibrationDimension + "^3]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av surface [" + calibrationDimension + "]^2" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av ramification index" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av spanned volume (convex hull) [" + calibrationDimension + "^3]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av spanned surface (convex hull) [" + calibrationDimension + "]^2" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av spanned volume center x (convex hull) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av spanned volume center y (convex hull) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av spanned volume center z (convex hull) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector x (binary) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector y (binary) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector z (binary) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector length (binary) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity index (binary)" + spacer;	legendLTtime += timeSpacer;
		
		legendLT += "	" + "av polarity vector x [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector y [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector z [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity vector length [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av polarity index" + spacer;	legendLTtime += timeSpacer;

		legendLT += "	" + "av # found skls" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # branches (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # junctions (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # tips (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # triple points (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # quadruple points (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # junction voxels (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # slab voxels (largest skl)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av tree length (largest skl) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av average branch length (largest skl) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av maximum branch length (largest skl) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av shortest path (largest skl) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # branches (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # junctions (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # tips (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # triple points (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # quadruple points (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # junction voxels (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # slab voxels (all skls)" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av tree length (all skls) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av average branch length (all skls) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av maximum branch length (all skls) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av shortest path (all skls) [" + calibrationDimension + "]" + spacer;	legendLTtime += timeSpacer;
		
		legendLT += "	" + "av moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av moving vector length [" + calibrationDimension + "/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av extended volume [" + calibrationDimension + "^3/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av retracted volume [" + calibrationDimension + "^3/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av shape dynamics (extended + retracted volume) [" + calibrationDimension + "^3/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av measured volume difference [" + calibrationDimension + "^3/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av measured ramification index difference [1/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # extensions [1/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # retractions [1/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # extensions (> 1 voxel) [1/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		legendLT += "	" + "av # retractions (> 1 voxel) [1/" + timeUnit + "]" + spacer;	legendLTtime += timeSpacer;
		
		tp.append(legendLT);
		tp.append(legendLTtime);
		tp.saveAs(savePath);
	}

	/**
	 * saves one-line results file containing the results for the short-term parameters
	 * */
	void saveOneRowResultsST(ArrayList<TimelapseParticle> particles, int frames, double xCorr, double yCorr, double zCorr, String directory, String name, String savePath){
		TextPanel tw3 =new TextPanel("One Row Results");
		for(int p = 0; p < particles.size(); p++){
			TimelapseParticle particle = particles.get(p);
			String appendTxtTw3 = "" + directory;
			appendTxtTw3 += "	" + name;
			appendTxtTw3 += "	" + dformat0.format(p+1);
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.xC [t] + xCorr*calibration);	//	"x center [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}			
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.yC [t] + yCorr*calibration);	//	"y center [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.zC [t] + zCorr*voxelDepth);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.xCOM [t] + xCorr*calibration);	//	"x center of mass [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.yCOM [t] + yCorr*calibration);	//	"y center of mass [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.zCOM [t] + zCorr*voxelDepth);	//	"z center of mass [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.xSpan [t]);	//	"x span [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.ySpan [t]);	//	"y span [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.zSpan [t]);	//	"z span [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.averageIntensity [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.minimumIntensity [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.maximumIntensity [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.sdIntensity [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.volume [t]);	//	"volume [" + calibrationDimension + "^2]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.surface [t]);	//	"surface [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.RI [t]);	//	"ramification index";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.convexHullVolume [t]);	//	"spanned volume (convex hull) [" + calibrationDimension + "^2]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.convexHullSurface [t]);	//	"spanned surface (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.convexHullxC [t] + xCorr*calibration);	//	"spanned volume center x (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.convexHullyC [t] + yCorr*calibration);	//	"spanned volume center y (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.convexHullzC [t] + zCorr*voxelDepth);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.xPolarityVectorBIN [t]);	//	"polarity vector x (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.yPolarityVectorBIN [t]);	//	"polarity vector y (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.zPolarityVectorBIN [t]);	//	"polarity vector z (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.polarityVectorLengthBIN [t]);	//	"polarity vector length (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.polarityIndexBIN [t]);	//	"polarity index (binary)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"polarity vector x [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.xPolarityVectorSemiBIN [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"polarity vector y [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.yPolarityVectorSemiBIN [t]);	
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"polarity vector z [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.zPolarityVectorSemiBIN [t]);	
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"polarity vector length [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.polarityVectorLengthSemiBIN [t]);	
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"polarity index";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.polarityIndexSemiBIN [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
		
//			"ID of largest skeleton (skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.IDofLargest [t]+1);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# found skls";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.foundSkl [t]);	
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# branches (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.branches [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# junctions (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.junctions [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# tips (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.tips [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# triple points (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.triplePs [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# quadruple points (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.quadruplePs [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# junction voxels (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.junctionVx [t]);			
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# slab voxels (largest skl)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.slabVx [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"tree length (largest skl) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.treeLength [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"average branch length (largest skl) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avBranchLength [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"maximum branch length (largest skl) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.maxBranchLength [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"shortest Path (largest skl)"
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.largestShortestPath [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# branches (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.branchesOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# junctions (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.junctionsOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# tips (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.tipsOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# triple points (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.triplePsOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# quadruple points (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.quadruplePsOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# junction voxels (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.junctionVxOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"# slab voxels (all skls)";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat0.format(particle.slabVxOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"tree length (all skls) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.treeLengthOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"average branch length (all skls) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avBranchLengthOfAll [t]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"maximum branch length (all skls) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.maxBranchLengthOfAll [t]);					
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
//			"shortest path (all skls) [" + calibrationDimension + "]";
			for(int t = 0; t < particle.times; t++){
				if(t>=particle.tMin&&t<=particle.tMax&&skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.largestShortestPathOfAll [t]);					
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.movingVectorLengthBIN [t]/timePerFrame);	//	"moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax&&semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.movingVectorLengthSemiBIN [t]/timePerFrame);	//	"moving vector length [" + calibrationDimension + "/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
		
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.occupVolume [t]/timePerFrame);					//	"extended volume [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.lostVolume [t]/timePerFrame);					//	"retracted volume [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.motility [t]/timePerFrame);					//	"shape dynamics (extended + retracted volume) [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.deltaVolume [t]/timePerFrame);					//	"measured volume difference [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.deltaRI [t]/timePerFrame);						//	"measured ramification index difference [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.nrOfExtensions [t]/timePerFrame);				//	"# extensions [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.nrOfRetractions [t]/timePerFrame);				//	"# retractions [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.nrOfExtensionsMoreThan1Vx [t]/timePerFrame);	//	"# extensions (> 1 voxel) [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int t = 0; t < particle.times; t++){
				if(t>particle.tMin&&t<=particle.tMax){
					appendTxtTw3 += "	" + dformat6.format(particle.nrOfRetractionsMoreThan1Vx [t]/timePerFrame);	//	"# retractions (> 1 voxel) [1/" + timeUnit + "]";		
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += ("	" 
			+ "	" + "Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by '"+PLUGINNAME+"',"
					+ " \u00a9 2014 - " + yearOnly.format(new Date()) + " Jan Niklas Hansen (jan.hansen@uni-bonn.de)."
			+ "	" + "The plugin '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
					+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
					+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE."
			+ "	" + "Plugin version: "+PLUGINVERSION);	
			tw3.append(appendTxtTw3);				
		}
		tw3.saveAs(savePath);
	}

	/**
	 * saves one-line results file containing the results for the long-term parameters
	 * */
	void saveOneRowResultsLT(ArrayList<TimelapseParticle> particles, int frames, double xCorr, double yCorr, double zCorr, String directory, String name, String savePath){
		TextPanel tw3 =new TextPanel("One Row Results");
		for(int p = 0; p < particles.size(); p++){
			TimelapseParticle particle = particles.get(p);
			String appendTxtTw3 = "" + directory;
			appendTxtTw3 += "	" + name;
			appendTxtTw3 += "	" + dformat0.format(p+1);
			
			int nrOfGroups = (int)((double)frames/(double)totalGroupSize);
			
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.projectedVolume [group]);	//	"projected volume (= 'scanned volume') [" + calibrationDimension + "^2/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.projectedStaticVolume [group]);	//	"static volume [" + calibrationDimension + "^2/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.projectedDynamicFraction [group]);	//	"dynamic fraction of projected volume (= 'scanning activity')";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.projectedConvexHullVolume [group]);	//	"projected spanned volume (convex hull) [" + calibrationDimension + "^2/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.projectedConvexHullStaticVolume [group]);	//	"static spanned volume (convex hull) [" + calibrationDimension + "^2/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.projectedConvexHullDynamicFraction [group]);	//	"dynamic fraction of spanned volume (convex hull) ";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.accumulatedDistanceBIN [group]);	//	"accumulated distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.euclideanDistanceBIN [group]);	//	"euclidean distance (binary center displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.directionalityIndexBIN [group]);	//	"directionality index = eucledian/accumulated distance (binary)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.accumulatedDistanceSemiBIN [group]);	//	"accumulated distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.euclideanDistanceSemiBIN [group]);	//	"euclidean distance (center of mass displacement) [" + calibrationDimension + "/" + groupTimeString + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.directionalityIndexSemiBIN [group]);	//	"directionality index = eucledian/accumulated distance (center of mass)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
		
			//averages per Group
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avXC [group]+xCorr*calibration);	//	"x center [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avYC [group]+yCorr*calibration);	//	"y center [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avZC [group]+zCorr*voxelDepth);	//	"z center [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avXCOM [group]+xCorr*calibration);	//	"x center of mass [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avYCOM [group]+yCorr*calibration);	//	"y center of mass [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avZCOM [group]+zCorr*voxelDepth);	//	"z center of mass [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avXSpan [group]);	//	"x span [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avYSpan [group]);	//	"y span [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avZSpan [group]);	//	"z span [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avAverageIntensity [group]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avMinimumIntensity [group]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avMaximumIntensity [group]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avSdIntensity [group]);
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avVolume [group]);	//	"volume [" + calibrationDimension + "^2]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avSurface [group]);	//	"surface [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avRI [group]);	//	"ramification index";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avConvexHullVolume [group]);	//	"spanned volume (convex hull) [" + calibrationDimension + "^2]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avConvexHullSurface [group]);	//	"spanned surface (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avConvexHullxC [group]+xCorr*calibration);	//	"spanned volume center x (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avConvexHullyC [group]+yCorr*calibration);	//	"spanned volume center y (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avConvexHullzC [group]+zCorr*voxelDepth);	//	"spanned volume center z (convex hull) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avXPolarityVectorBIN [group]);	//	"polarity vector x (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avYPolarityVectorBIN [group]);	//	"polarity vector y (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avPolarityVectorLengthBIN [group]);	//	"polarity vector length (binary) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avPolarityIndexBIN [group]);	//	"polarity index (binary)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avXPolarityVectorSemiBIN [group]);	//	"polarity vector x [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avYPolarityVectorSemiBIN [group]);	//	"polarity vector y [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avPolarityVectorLengthSemiBIN [group]);	//	"polarity vector length [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			} 
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avPolarityIndexSemiBIN [group]);	//	"polarity index";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avFoundSkl [group]);				//	"# found skls";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avBranches [group]);				//	"# branches (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avJunctions [group]);			//	"# junctions (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avTips [group]);					//	"# tips (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avTriplePs [group]);				//	"# triple points (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avQuadruplePs [group]);			//	"# quadruple points (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}			
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avJunctionVx [group]);			//	"# junction voxels (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avSlabVx [group]);				//	"# slab voxels (largest skl)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avTreeLength [group]);			//	"tree length (largest skl) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avAvBranchLength [group]);		//	"average branch length (largest skl) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avMaxBranchLength [group]);		//	"maximum branch length (largest skl) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avLargestShortestPath [group]);		//	"shortest path (largest skl) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avBranchesOfAll [group]);		//	"# branches (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avJunctionsOfAll [group]);		//	"# junctions (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avTipsOfAll [group]);			//	"# tips (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avTriplePsOfAll [group]);		//	"# triple points (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avQuadruplePsOfAll [group]);		//	"# quadruple points (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avJunctionVxOfAll [group]);		//	"# junction voxels (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avSlabVxOfAll [group]);			//	"# slab voxels (all skls)";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avTreeLengthOfAll [group]);		//	"tree length (all skls) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avAvBranchLengthOfAll [group]);	//	"average branch length (all skls) [" + calibrationDimension + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avMaxBranchLengthOfAll [group]);	//	"maximum branch length (all skls) [" + calibrationDimension + "]";					
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && skeletonize){
					appendTxtTw3 += "	" + dformat6.format(particle.avLargestShortestPathOfAll [group]);	//	"shortest path (all skls) [" + calibrationDimension + "]";					
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avMovingVectorLengthBIN [group]/timePerFrame);	//	"moving vector length (binary) [" + calibrationDimension + "/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
		
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes && semiBinary){
					appendTxtTw3 += "	" + dformat6.format(particle.avMovingVectorLengthSemiBIN [group]/timePerFrame);	//	"moving vector length [" + calibrationDimension + "/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avOccupVolume [group]/timePerFrame);					//	"extended volume [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avLostVolume [group]/timePerFrame);					//	"retracted volume [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avMotility [group]/timePerFrame);					//	"shape dynamics (extended + retracted volume) [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avDeltaVolume [group]/timePerFrame);					//	"measured volume difference [" + calibrationDimension + "^2/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avDeltaRI [group]/timePerFrame);						//	"measured ramification index difference [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avNrOfExtensions [group]/timePerFrame);				//	"# extensions [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avNrOfRetractions [group]/timePerFrame);				//	"# retractions [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avNrOfExtensionsMoreThan1Px [group]/timePerFrame);	//	"# extensions (> 1 voxel) [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			} 
			appendTxtTw3 += "	";
			for(int group = 0; group < nrOfGroups; group++){
				if(group < particle.projectedTimes){
					appendTxtTw3 += "	" + dformat6.format(particle.avNrOfRetractionsMoreThan1Px [group]/timePerFrame);	//	"# retractions (> 1 voxel) [1/" + timeUnit + "]";
				}else{
					appendTxtTw3 += "	";
				}
			}
			appendTxtTw3 += ("	" 
					+ "	" + "Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by '"+PLUGINNAME+"',"
							+ " \u00a9 2014 - " + yearOnly.format(new Date()) + " Jan Niklas Hansen (jan.hansen@uni-bonn.de)."
					+ "	" + "The plugin '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
							+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
							+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE."
					+ "	" + "Plugin version: "+PLUGINVERSION);		
			tw3.append(appendTxtTw3);
		}
		tw3.saveAs(savePath);
	}

	/**
	 * @return maximum intensity value a pixel can be of in the ImagePlus
	 * @param imp
	 * */
	static int getMaxIntensity(ImagePlus imp){
		int maxThreshold = 0;
		if(imp.getBitDepth()==8){
			maxThreshold = 255;
		}else if(imp.getBitDepth()==16){
			maxThreshold = 65535;
		}else if(imp.getBitDepth()==32){
			maxThreshold = 2147483647;
		}else{
//			IJ.error("Error! No gray scale image!",ProgressDialog.ERROR);
		}
		return maxThreshold;	
	}
	}