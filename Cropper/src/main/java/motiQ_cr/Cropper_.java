/**===============================================================================
 * 
 * MotiQ_cropper plugin for ImageJ, Version v0.1.2
 * 
 * Copyright (C) 2014-2023 Jan N. Hansen
 * First version: December 01, 2014 
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
 * ==============================================================================*/

package motiQ_cr;

import java.awt.*;
import java.awt.Font;
import java.util.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.text.*;
import java.text.*;

public class Cropper_ implements PlugIn, Measurements {
	static final String PLUGINNAME = "MotiQ_cropper";
	static final String PLUGINVERSION = "v0.1.2";
	static final SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
	
	//Fonts
		static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
		static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
		static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
		static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	
	//variables
		static final String roiPrefix = "Polygon_Stack_";//

	//Selection variables
		static final String[] taskVariant = {"process the active, open image","manually open image to be processed"};
		String selectedTaskVariant = taskVariant[0];
		
		static final String[] stackVariants = {"consecutive order (recommended for time-lapse 2D images)",
				"user-defined order (recommended for time-lapse and non-time-lapse 3D images)"};
		String selectedStackVariant = stackVariants [0];
		
public void run(String arg) {
	
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//-------------------------GenericDialog--------------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	GenericDialog gd = new GenericDialog(PLUGINNAME + " - settings");
	//show Dialog-----------------------------------------------------------------
	gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", version " + PLUGINVERSION 
			+ " (\u00a9 2014 - " + yearOnly.format(new Date()) + ", Jan Niklas Hansen)", SuperHeadingFont);
	gd.setInsets(0,0,0);	gd.addChoice("Image:", taskVariant, selectedTaskVariant);
	gd.setInsets(5,0,0);	gd.addChoice("Process stack in ", stackVariants, selectedStackVariant);
	gd.setInsets(10,0,0);	gd.addCheckbox("Post processing, crop image to selected region", true);

	gd.setInsets(10,0,0);
	gd.showDialog();
	//show Dialog-----------------------------------------------------------------

	//read and process variables--------------------------------------------------
	String selectedTaskVariant = gd.getNextChoice();
	
	selectedStackVariant = gd.getNextChoice();
	boolean freeStackOrder = true;
	if(selectedStackVariant.equals(stackVariants[0]))	freeStackOrder = false;
	
	boolean cropImage = gd.getNextBoolean();
	//read and process variables--------------------------------------------------
	
	if (gd.wasCanceled()) return;

//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//---------------------end-GenericDialog-end----------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	ImagePlus imp = new ImagePlus();
	String name = new String();
	String dir = new String();
	
	//get folder and file name
	if (selectedTaskVariant.equals(taskVariant[1])){
		OpenDialog od = new OpenDialog("List Opener", null);
		name = od.getFileName();
		dir = od.getDirectory();
		imp = IJ.openImage(""+dir+name+"");
	   	imp.show();			   	
	}else if (selectedTaskVariant.equals(taskVariant[0])){
		imp = WindowManager.getCurrentImage();		
		FileInfo info = imp.getOriginalFileInfo();
		try {
			name = info.fileName;	//get name
			dir = info.directory;	//get directory			
		}catch(Exception e) {
			IJ.error("MotiQ Cropper can only process an image that is saved. Save the image and relaunch MotiQ Cropper to process it.");
			return;
		}
	}
	  
	final ImagePlus impSave = imp.duplicate();
	impSave.hide();
	final String impTitle = imp.getTitle();
	
	
	//Get Image Properties
	int width = imp.getWidth(), 
			height = imp.getHeight(), 
			stacksize = imp.getStackSize(),
			slices = imp.getNSlices(),
			frames = imp.getNFrames(),
			channels = imp.getNChannels();
	
	//check for hyperstack
	boolean hyperStack = true;
	if(channels == stacksize || frames == stacksize || slices == stacksize)	hyperStack = false;	
	
	//Generate backup image array
	double backupImage [][][] = new double [width][height][stacksize];
	for(int z = 0; z < stacksize; z++){
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				backupImage [x][y][z] = imp.getStack().getVoxel(x,y,z);
			}
		}
	}
	
	//Init ROI Manager
	RoiManager rm = RoiManager.getInstance();
	if (rm==null) rm = new RoiManager();
	rm.runCommand("reset");
		
	running: while(true){
		//Process Saving Name
		SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
		Date currentDate = new Date();
		
		String namewithoutending = name;
		if(name.contains(".")){
			namewithoutending = name.substring(0,name.lastIndexOf("."));//Remove Datatype-Ending from name
		}

		String Dataname = ""+dir+namewithoutending+"_CUT_" + NameDateFormatter.format(currentDate) + "_Info.txt";
		String DatanameImp = ""+dir+namewithoutending+"_CUT_" + NameDateFormatter.format(currentDate) + ".tif";
		//Process Saving Name
		
		Roi roi;
//		imp.deleteRoi();
				
		int aMaxX [] = new int [stacksize], aMaxY [] = new int [stacksize], aMinX [] = new int [stacksize], aMinY [] = new int [stacksize], aMaxZ = 0, aMinZ = (stacksize-1);
		
		for(int i = 0; i < stacksize; i++){
			aMaxX [i] = 0;
			aMaxY [i] = 0;
			aMinX [i] = width-1;
			aMinY [i] = height-1;
		}
		
		rm.runCommand("reset");
				
		int startStacking = 0;
		if(freeStackOrder==false){
			SettingRois: while(true){
			Stacking: for (int stack = startStacking; stack < stacksize; stack++){
				IJ.selectWindow(imp.getID());
				IJ.setTool("polygon");	
				imp.setPosition(stack+1);		
				
				new WaitForUserDialog("Draw a ROI containing the cell of interest for stack image " + (stack+1) + "!").show();
				
				//start Generic Dialog for navigation	
					//Initialize variables for continue cutting dialog
					String [] continueVariants = new String [stack + 2 + 4];
					continueVariants [0] = "remove remaining stack images, save, and finish";
					continueVariants [1] = "apply the current ROI in all remaining stack images, save, and finish";
					continueVariants [2] = "keep remaining stack images as is, save, and finish";
					continueVariants [3] = "CONTINUE setting ROIs in next stack image (" + (stack+2) + ")";
					for (int i = stack+1; i > 0; i--){
					continueVariants [3 + (stack+2 - i)] = "restart setting ROIs from stack image " + i;	
					}						
					continueVariants [stack + 2 + 3] = "abort plugin";		
						
					//show Dialog
					GenericDialog gd2 = new GenericDialog(PLUGINNAME + ", version " + PLUGINVERSION);	
					gd2.hideCancelButton();
					gd2.setInsets(0,0,0);	gd2.addMessage(PLUGINNAME + ", version " + PLUGINVERSION 
							+ " (\u00a9 2014 - " + yearOnly.format(new Date()) + ", Jan Niklas Hansen)", SuperHeadingFont);
					gd2.setInsets(5,0,0);	gd2.addMessage("You currently have cropped image " + (stack+1) + "/" + stacksize + ".", InstructionsFont);
					gd2.setInsets(5,0,0);	gd2.addMessage((stacksize - stack - 1) + " stack images remaining to process.", InstructionsFont);
					gd2.setInsets(5,0,0);	gd2.addChoice("", continueVariants, continueVariants [3]);
					gd2.setInsets(5,0,0);	gd2.addMessage("Note: if you restart in a previous stack image, all following stack images are reset.", InstructionsFont);
					gd2.showDialog();
					
					//get Selections	
					int continueVariantIndex = gd2.getNextChoiceIndex();					
				//end GenericDialog for navigation					
					
				roi = imp.getRoi();		
			
				if(continueVariantIndex == continueVariants.length-1){
					//abort
					break running;
				}else if(continueVariantIndex > 3){
					//restart from previous frame
					int continueNr = (stack+2-(continueVariantIndex-3));
										
					if(continueNr == stack+1){
						imp.setPosition(stack+1);
					}else{
						for(int z = (continueNr-1); z < stack; z++){
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									imp.getStack().setVoxel(x,y,z,backupImage[x][y][z]);
								}
							}
							//Reset of parameters
							aMaxX [z] = 0;
							aMaxY [z] = 0;
							aMinX [z] = width-1;
							aMinY [z] = height-1;
							
							//RoiManager resetten
							for(int r = (rm.getCount()-1); r >= 0; r--){
								String roiName = rm.getName(r);	
								if(roiName.equals(roiPrefix + (z+1) + "")){
									rm.runCommand(imp,"Deselect");
									rm.select(r);
									rm.runCommand(imp,"Delete");
								}									
							}
						}
					}
					aMaxZ = continueNr-2;						
					
					if(roi!=null){
						imp.setRoi(new PolygonRoi(roi.getPolygon(),Roi.POLYGON));
					}
					
					startStacking = (continueNr-1);
					break Stacking;
				}
								
				//----------------------Extract Imagepart---------------------------
				if(roi!=null){
					Polygon p = roi.getPolygon();
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							double pxintensity = imp.getStack().getVoxel(x,y,stack);
							if(p.contains(x,y)==false&&pxintensity!=0){	
								imp.getStack().setVoxel( x, y, stack, 0.0);
							}else if(p.contains(x,y)){
								if(aMinX [stack] > x){aMinX[stack]=x;}
								if(aMaxX [stack] < x){aMaxX[stack]=x;}
								if(aMinY [stack] > y){aMinY[stack]=y;}
								if(aMaxY [stack] < y){aMaxY[stack]=y;}		
								
							}
						}
					}
					if(aMinZ > stack){aMinZ=stack;}
					if(aMaxZ < stack){aMaxZ=stack;}	
				}else{
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							imp.getStack().setVoxel( x, y, stack, 0.0);
						}
					}
//					IJ.log("No ROI was selected in image " + stack);
				}
				//----------------------Extract Imagepart---------------------------
					
				if(roi!=null){
					rm.add(imp, roi, rm.getCount());
					rm.select(imp, rm.getCount()-1);
					rm.runCommand("Rename", roiPrefix + (stack+1) + "");
				}
				
				if(stack == stacksize-1){
					//stop setting ROIs, if all images are processed
					break SettingRois;
				}
				
				if(continueVariantIndex == 1 && roi!=null){
					//Finish and apply current ROI to all remaining stack images
					Polygon p = roi.getPolygon();
					for (int stackRoi = stack+1; stackRoi < stacksize; stackRoi++){
						if(roi!=null){
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									double pxintensity = imp.getStack().getVoxel(x,y,stackRoi);
									if(p.contains(x,y)==false&&pxintensity!=0){	
										imp.getStack().setVoxel( x, y, stackRoi, 0.0);
									}else if(p.contains(x,y)){
										if(aMinX [stackRoi] > x){aMinX[stackRoi]=x;}
										if(aMaxX [stackRoi] < x){aMaxX[stackRoi]=x;}
										if(aMinY [stackRoi] > y){aMinY[stackRoi]=y;}
										if(aMaxY [stackRoi] < y){aMaxY[stackRoi]=y;}		
										
									}
								}
							}
							if(aMinZ > stackRoi){aMinZ=stackRoi;}
							if(aMaxZ < stackRoi){aMaxZ=stackRoi;}
							
							rm.add(imp, roi, rm.getCount());
							rm.select(imp, rm.getCount()-1);
							rm.runCommand("Rename", roiPrefix + (stackRoi+1) + "");
						}else{
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									imp.getStack().setVoxel( x, y, stackRoi, 0.0);
								}
							}
//							IJ.log("No ROI was selected in image " + stackRoi);
						}
					}
					break SettingRois;
				}else if(continueVariantIndex == 0
						|| (continueVariantIndex == 1 && roi==null)){	
					//Finish and remove remaining stack images
					if(cropImage==false){
						for(int z = stack+1; z < stacksize; z++){
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									imp.getStack().setVoxel(x,y,z,0.0);
								}
							}
						}
					}
					break SettingRois;
				}else if(continueVariantIndex == 2){
					//Keep remaining stack images and finish
					for (int stackRoi = stack+1; stackRoi < stacksize; stackRoi++){
						aMinX[stackRoi]=0;
						aMaxX[stackRoi]=imp.getWidth()-1;
						aMinY[stackRoi]=0;
						aMaxY[stackRoi]=imp.getHeight()-1;	
						if(aMinZ > stackRoi){aMinZ=stackRoi;}
						aMaxZ=stackRoi;
					}
					break SettingRois;
				}				
				imp.updateImage();
			}
			}
		}else{
			//list of processed images
			boolean imageCropped [] = new boolean [stacksize];
			for(int i = 0; i < stacksize; i++){
				imageCropped [i] = false;
			}
			
			//Variables for intermediate dialog
			int continueIndex = 5;
			String [] continueVariants;
			if(hyperStack){			
				continueVariants = new String [7];
				continueVariants [5] = "CONTINUE setting ROIs in closest remaining slice";
				continueVariants [6] = "CONTINUE setting ROIs in closest remaining frame";				
			}else{
				continueVariants = new String [6];
				continueVariants [5] = "CONTINUE setting ROIs in closest remaining stack image";
			}
			continueVariants [0] = "abort plugin";
			continueVariants [1] = "clear remaining stack images, save, and finish";
			continueVariants [2] = "apply the current ROI in all remaining stack images, save, and finish";
			continueVariants [3] = "keep remaining stack images as is, save, and finish";
			continueVariants [4] = "CONTINUE setting ROIs but stay at current stack position";
			
			SettingRois: while(true){
				IJ.setTool("polygon");	
				IJ.selectWindow(imp.getID());
				int stack = 0;
				int cSlice = imp.getSlice()-1, 
					cFrame = imp.getFrame()-1, 
					cChannel = imp.getChannel()-1;
				
				waitingDialog: while(true){
					new WaitForUserDialog("Draw a ROI in your preferred slice!").show();
					cSlice = imp.getSlice()-1; 
					cFrame = imp.getFrame()-1; 
					cChannel = imp.getChannel()-1;
					roi = imp.getRoi();					
					stack = imp.getCurrentSlice()-1;
					
					if(imageCropped[stack]==true){
						YesNoCancelDialog confirm = new YesNoCancelDialog(IJ.getInstance(),"Action required", "Previously, you had already set a ROI for stack image " + (stack+1) + ". Do you want to reset the image using the new ROI?");
						if (confirm.yesPressed()){	
							imageCropped[stack] = false;
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									imp.getStack().setVoxel(x,y,stack,backupImage[x][y][stack]);
								}
							}
							//Reset of parameters
							aMaxX [stack] = 0;
							aMaxY [stack] = 0;
							aMinX [stack] = width-1;
							aMinY [stack] = height-1;
							
							//Reset RoiManager
							for(int z = 0; z < rm.getCount(); z++){								
								String roiName = rm.getName(z);
								if(roiName.contains(roiPrefix)){
									int stackRoiNr = Integer.parseInt(roiName.substring(roiPrefix.length()));
									if(stackRoiNr==(stack+1)){
										rm.runCommand(imp,"Deselect");
										rm.select(z);
										rm.runCommand(imp,"Delete");
									}
								}																
							}					
							
							break waitingDialog;
						}
					}else{
						break waitingDialog;
					}					
				}
				imageCropped [stack] = true;
				
				
				//----------------------Extract Imagepart---------------------------
				if(roi!=null){
					Polygon p = roi.getPolygon();
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							double pxintensity = imp.getStack().getVoxel(x,y,stack);
							if(p.contains(x,y)==false&&pxintensity!=0.0){	
								
								imp.getStack().setVoxel(x, y, stack, 0.0);
							}else if(p.contains(x,y)){
								if(aMinX [stack] > x){aMinX[stack]=x;}
								if(aMaxX [stack] < x){aMaxX[stack]=x;}
								if(aMinY [stack] > y){aMinY[stack]=y;}
								if(aMaxY [stack] < y){aMaxY[stack]=y;}		
								
							}
						}
					}
				}else{
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							imp.getStack().setVoxel(x, y, stack, 0.0);
						}
					}
//					IJ.log("No ROI was selected in image " + (stack+1));
				}
				//----------------------Extract Imagepart---------------------------
					
				if(roi!=null){
					rm.add(imp, roi, rm.getCount());
					rm.select(imp, rm.getCount()-1);
					rm.runCommand("Rename", roiPrefix + (stack+1) + "");
				}
					
				// Generic Dialog for navigation	
					//Initialize variables for continue cutting dialog
						//Count remaining frames
						int cutImgCt = 0;
						for(int i = 0; i<stacksize; i++){
							if(imageCropped[i]){
								cutImgCt++;
							}
						}
						
						//Finish analysis if all images have been cut
						if(cutImgCt == stacksize)	break SettingRois;
						
						//generate list of cut images
						String [] croppedImagesOverview = new String [cutImgCt];
						int cutImgCt2 = 0;
						for(int i = 1; i <= stacksize; i++){
							if(imageCropped[i-1]){
								croppedImagesOverview [cutImgCt2] = "" + (i);
								cutImgCt2++;
							}
						}
						
					//show Dialog
					GenericDialog gd2 = new GenericDialog(PLUGINNAME + ", version " + PLUGINVERSION);	
					gd2.hideCancelButton();
					gd2.setInsets(0,0,0);	gd2.addMessage(PLUGINNAME + ", version " + PLUGINVERSION 
							+ " (\u00a9 2014 - " + yearOnly.format(new Date()) + ", Jan Niklas Hansen)", SuperHeadingFont);
					gd2.setInsets(5,0,0);	gd2.addMessage("You currently cropped image " + (stack+1) + "/" + stacksize + ".", InstructionsFont);
					gd2.setInsets(5,0,0);	gd2.addMessage((stacksize - stack - 1) + " stack images remaining to process.", InstructionsFont);
					gd2.setInsets(5,0,0);	gd2.addChoice("", continueVariants, continueVariants [continueIndex]);
					
					gd2.setInsets(15,0,0);	gd2.addCheckbox("Reset specific stack image", false);
					gd2.setInsets(-25,80,0);	gd2.addChoice("", croppedImagesOverview , croppedImagesOverview[0]);
															
					gd2.showDialog();
					
					//read and process variables					
					continueIndex = gd2.getNextChoiceIndex();
					boolean resetFrame = gd2.getNextBoolean();	
					int resetNr = Integer.parseInt(gd2.getNextChoice())-1;
				// Generic Dialog for navigation
			
				if(continueIndex == 0){
					//abort
					break running;
				}					
				if(continueIndex==1
						|| (continueIndex==2 && roi==null)){
					//clear remaining stack images, save and finish
					for(int i = 0; i<stacksize; i++){
						if(imageCropped[i]==false){
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									imp.getStack().setVoxel(x,y,i,0.0);
								}
							}
						}
					}
					break SettingRois;
				}	
				
				if(continueIndex==2 && roi!=null){
					//apply the current ROI in all remaining stack images, save, and finish"
					for(int i = 0; i < stacksize; i++){
						if(imageCropped [i]==false){
							Polygon p = roi.getPolygon();
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									if(p.contains(x,y)==false){	
										imp.getStack().setVoxel(x, y, i, 0.0);
									}else if(p.contains(x,y)){
										if(aMinX [i] > x){aMinX [i] = x;}
										if(aMaxX [i] < x){aMaxX [i] = x;}
										if(aMinY [i] > y){aMinY [i] = y;}
										if(aMaxY [i] < y){aMaxY [i] = y;}											
									}
								}
							}
							imageCropped [i] = true;
							
							if(roi!=null){
								rm.add(imp, roi, rm.getCount());
								rm.select(imp, rm.getCount()-1);
								rm.runCommand("Rename", roiPrefix + (i+1) + "");
							}
						}
					}
					break SettingRois;
				}
				
				if(continueIndex==3){
					//keep remaining stack images as is, save and finish
					for(int i = 0; i<stacksize; i++){
						if(imageCropped[i]==false){
							aMinX[i]=0;
							aMaxX[i]=imp.getWidth();
							aMinY[i]=0;
							aMaxY[i]=imp.getHeight();								
							imageCropped [i] = true;
						}
					}
					break SettingRois;
				}
				
				if(resetFrame){
					imp.setPosition(resetNr+1);						
					imageCropped[resetNr] = false;
						
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							imp.getStack().setVoxel(x,y,resetNr,backupImage[x][y][resetNr]);
						}
					}
					//Reset of parameters
					aMaxX [resetNr] = 0;
					aMaxY [resetNr] = 0;
					aMinX [resetNr] = width-1;
					aMinY [resetNr] = height-1;
					
					//Rest RoiManager
					for(int z = 0; z < rm.getCount(); z++){
						String roiName = rm.getName(z);
						if(roiName.contains(roiPrefix)){
							int stackRoiNr = Integer.parseInt(roiName.substring(roiPrefix.length()));
							if(stackRoiNr==(resetNr+1)){
								rm.runCommand(imp,"Deselect");
								rm.select(z);
								rm.runCommand(imp,"Delete");
							}
						}														
					}
											
					imp.setRoi(roi);
				}
				
				if(continueIndex == 5){	
					//Auto-moving to next remaining slice or stack image
					moving: for(int i = 0; i < stacksize; i++){
						if(stack+i<stacksize){
							if(imageCropped[stack+i]==false){
								imp.setPosition(stack+i+1);
								break moving;
							}							
						}
						if(stack-i>=0){
							if(imageCropped[stack-i]==false){
								imp.setPosition(stack-i+1);
								break moving;
							}							
						}						
					}
				}else if(continueIndex == 6){	
					//Auto-moving to next remaining frame
					boolean found = false;
					search: while(true){
						moving: for(int i = 0; i < frames; i++){
							if(stack+i*slices<stacksize){
								if(imageCropped[stack+i*slices]==false){
									imp.setPosition(stack+i*slices+1);
									found = true;
									break moving;
								}							
							}
							if(stack-i*slices>=0){
								if(imageCropped[stack-i*slices]==false){
									imp.setPosition(stack-i*slices+1);
									found = true;
									break moving;
								}							
							}						
						}
						if(found){
							break search;
						}else{	
							//move to next remaining stack
							moving: for(int i = 0; i < stacksize; i++){
								if(stack+i<stacksize){
									if(imageCropped[stack+i]==false){
										imp.setPosition(stack+i+1);
										break moving;
									}							
								}
								if(stack-i>=0){
									if(imageCropped[stack-i]==false){
										imp.setPosition(stack-i+1);
										break moving;
									}							
								}						
							}
						}
					}						
				}
				
				//if image is a hyperstack, search for the closest frame's / slice's / channel's ROI
				if(hyperStack){
					cFrame = imp.getFrame(); 
					cChannel = imp.getChannel();
					cSlice = imp.getSlice();
					int z;
					searching: for(int f = 1; f+cFrame <= frames || cFrame-f > 0; f++){
						for(int c = 0; cChannel + c <= channels || cChannel-c > 0; c++){
							for(int j = 0; j < rm.getCount(); j++){
								z = imp.getStackIndex(cChannel+c, cSlice, cFrame+f);
								if(rm.getRoi(j).getName().equals(roiPrefix + (z) + "")){
									imp.setRoi(new PolygonRoi(rm.getRoi(j).getPolygon(),Roi.POLYGON));
									break searching;
								}
								z = imp.getStackIndex(cChannel-c, cSlice, cFrame+f);
								if(rm.getRoi(j).getName().equals(roiPrefix + (z) + "")){
									imp.setRoi(new PolygonRoi(rm.getRoi(j).getPolygon(),Roi.POLYGON));
									break searching;
								}
								z = imp.getStackIndex(cChannel-c, cSlice, cFrame-f);
								if(rm.getRoi(j).getName().equals(roiPrefix + (z) + "")){
									imp.setRoi(new PolygonRoi(rm.getRoi(j).getPolygon(),Roi.POLYGON));
									break searching;
								}
								z = imp.getStackIndex(cChannel+c, cSlice, cFrame-f);
								if(rm.getRoi(j).getName().equals(roiPrefix + (z) + "")){
									imp.setRoi(new PolygonRoi(rm.getRoi(j).getPolygon(),Roi.POLYGON));
									break searching;
								}
							}
						}														
					}					
				}else{
					imp.setRoi(new PolygonRoi(rm.getRoi(rm.getCount()-1).getPolygon(),Roi.POLYGON));
				}			
				imp.updateImage();
			}	
			
			for(int z = 0; z < stacksize; z++){
				boolean zTrue = false;
				for(int x = 0; x < width; x++){
					for(int y = 0; y < height; y++){
						if(imp.getStack().getVoxel(x,y,z)!=0.0){
							zTrue = true;
						}
					}
				}
				if(zTrue){
					if(aMinZ > z){aMinZ=z;}
					if(aMaxZ < z){aMaxZ=z;}	
				}				
			}
		}
				
		if(cropImage){
			int max_x = 0, max_y = 0, min_x  = width-1, min_y  = height-1, max_z = aMaxZ, min_z = aMinZ;
			for(int i = 0; i < stacksize; i++){
				if(min_x > aMinX[i]){min_x=aMinX[i];}
				if(max_x < aMaxX[i]){max_x=aMaxX[i];}
				if(min_y > aMinY[i]){min_y=aMinY[i];}
				if(max_y < aMaxY[i]){max_y=aMaxY[i];}		
			}
			
			if(min_x > 0)			{min_x--;}
			if(max_x < (width-1))	{max_x++;}
			if(min_y > 0)			{min_y--;}
			if(max_y < (height-1))	{max_y++;}
						
			imp.deleteRoi();
			Roi RoiCut = new Roi(min_x, min_y, max_x-min_x+1, max_y-min_y+1);
			IJ.selectWindow(imp.getID());	
			imp.setRoi(RoiCut);			
			IJ.run(imp, "Crop", "");
			
			imp.getCalibration().xOrigin = imp.getCalibration().xOrigin + min_x;
			imp.getCalibration().yOrigin = imp.getCalibration().yOrigin + min_y;
			
			if(!hyperStack){
				imp.getCalibration().zOrigin = imp.getCalibration().zOrigin + min_z;
				for(int stack = stacksize-1; stack >= 0; stack--){
					if(stack<min_z){
						IJ.selectWindow(imp.getID());
						imp.setPosition(stack+1);			
						IJ.run(imp, "Delete Slice", "");
					}
					if(stack>max_z){
						IJ.selectWindow(imp.getID());
						imp.setPosition(stack+1);			
						IJ.run(imp, "Delete Slice", "");
					}
				}
			}						
			
			SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			TextPanel tp =new TextPanel("Metadata");
			tp.append("Cropping started: " + FullDateFormatter.format(currentDate));
			if(freeStackOrder){
				tp.append("During cropping free stack order mode was used.");
			}
			tp.append("Image Cropping Info (Min Values have to be added to any pixel position in the new image "
					+ "to find positions in the original image)");
			tp.append("	Min X:	" + min_x + "	Max X:	" + max_x);
			tp.append("	Min Y:	" + min_y + "	Max Y:	" + max_y);
			if(!hyperStack){
				tp.append("	Min Z:	" + min_z + "	Max Z:	" + max_z + "	(0 = smallest)");
			}else{
				tp.append("");
			}
			tp.append("");
			tp.append("Datafile was generated by '"+PLUGINNAME+"', \u00a9 2014 - " + yearOnly.format(new Date()) + " Jan Niklas Hansen (jan.hansen@caesar.de).");
			tp.append("The plugin '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
					+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
					+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
			tp.append("Plugin version:	"+PLUGINVERSION);			
			tp.saveAs(Dataname);			
			
			DatanameImp = ""+dir+namewithoutending+"_CUT_" + NameDateFormatter.format(currentDate) + "_X" + min_x + "_Y" + min_y + "_Z" + min_z + ".tif";
		}
		
		imp.deleteRoi();
		IJ.saveAsTiff(imp,DatanameImp);
		
		// Save RoiSet
			String Roi_Dataname = ""+dir+namewithoutending+"_CUT_" + NameDateFormatter.format(currentDate) + "_RoiSet.zip";
			rm.runCommand("Save", Roi_Dataname);
		// Save RoiSet	
		
		imp.changes = false;
		imp.close();
		YesNoCancelDialog confirm = new YesNoCancelDialog(IJ.getInstance(),"Do it again?", "Your selection was saved. Do you want to cut out another region in the same stack?");
		if (confirm.yesPressed()){
			imp = impSave.duplicate();
			imp.setTitle(impTitle);
			imp.show();
		}else{break running;}
	}	
}
}
