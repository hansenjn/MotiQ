/***===============================================================================
 * 
 * MotiQ_3D Version plugin for ImageJ, Version v0.1.3
 * 
 * Copyright (C) 2014-2017 Jan Niklas Hansen
 * First version: July 28, 2014 
 * This Version: December 1, 2017
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
 * For any questions please feel free to contact me (jan.hansen@caesar.de).
 * 
 * ===========================================================================**/

package motiQ3D;

import java.awt.Polygon;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.RGBStackConverter;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import Skeletonize3D_.Skeletonize3D_;
import skeleton_analysis.AnalyzeSkeleton_;
import skeleton_analysis.SkeletonResult;

public class TimelapseParticle{
	public boolean initialized = false;
	double pointList [][];	//[nr][x,y,time]
	double data [][][][];
	boolean hullData [][][][];
	int tMin, tMax, times, xMin, xMax, width, yMin, yMax, height, zMin, zMax, slices, projectedTimes;		
	int time [];
	double vxVolume;
	
	//Morphological static parameters
	public ImagePlus convexHullImp, particleImp;
	double xC [], yC [], zC [],
	xCOM [], yCOM [], zCOM [],
	xSpan [], ySpan [], zSpan [],
	averageIntensity [],
	minimumIntensity [],	
	maximumIntensity [],
	sdIntensity [],
	volume [], 
	surface [],
	RI [],
	convexHullVolume [],
	convexHullSurface [],
	convexHullxC [], convexHullyC [], convexHullzC [],
	xPolarityVectorBIN [], yPolarityVectorBIN [], zPolarityVectorBIN [],
	polarityVectorLengthBIN [],
	polarityIndexBIN [], 
	xPolarityVectorSemiBIN [], yPolarityVectorSemiBIN [], zPolarityVectorSemiBIN [],
	polarityVectorLengthSemiBIN [],
	polarityIndexSemiBIN [];
	
	//Skeleton parameters
	public ImagePlus skeletonImp = null;
	int IDofLargest [],
	foundSkl [],
	branches [],
	junctions [],
	tips [],
	triplePs [],
	quadruplePs [],
	junctionVx [],
	slabVx [],
	branchesOfAll [],
	junctionsOfAll [],
	tipsOfAll [],
	triplePsOfAll [],
	quadruplePsOfAll [],
	junctionVxOfAll [],
	slabVxOfAll [];
	double treeLength [],
	avBranchLength [],
	maxBranchLength [],
	largestShortestPath [],
	treeLengthOfAll [],
	avBranchLengthOfAll [],
	maxBranchLengthOfAll [],
	largestShortestPathOfAll [];
	
	//Dynamic parameters
	double movingVectorLengthBIN [],
	movingVectorLengthSemiBIN [],
	occupVolume [],
	lostVolume [],
	motility [],
	deltaVolume [],
	deltaRI [];
	int nrOfExtensions [],
	nrOfRetractions [],
	nrOfExtensionsMoreThan1Vx [],
	nrOfRetractionsMoreThan1Vx [];
			
	//Long term dynamic parameters
	double projectedVolume [],
	projectedStaticVolume [],
	projectedDynamicFraction [],
	projectedConvexHullVolume [],
	projectedConvexHullStaticVolume [],
	projectedConvexHullDynamicFraction [];	
	double accumulatedDistanceBIN [],
	euclideanDistanceBIN [],
	directionalityIndexBIN [],
	accumulatedDistanceSemiBIN [],
	euclideanDistanceSemiBIN [],
	directionalityIndexSemiBIN [];
	
	//Long term averages of parameters
	double avXC [], avYC [], avZC [], 
	avXCOM [], avYCOM [], avZCOM [],
	avXSpan [], avYSpan [], avZSpan [],
	avAverageIntensity [],
	avMinimumIntensity [],
	avMaximumIntensity [],
	avSdIntensity [],
	avVolume [], 
	avSurface [],
	avRI [],
	avConvexHullVolume [],
	avConvexHullSurface [],
	avConvexHullxC [], avConvexHullyC [], avConvexHullzC [],
	avXPolarityVectorBIN [], avYPolarityVectorBIN [], avZPolarityVectorBIN [],
	avPolarityVectorLengthBIN [],
	avPolarityIndexBIN [], 
	avXPolarityVectorSemiBIN [], avYPolarityVectorSemiBIN [], avZPolarityVectorSemiBIN [],
	avPolarityVectorLengthSemiBIN [],
	avPolarityIndexSemiBIN [];

	double avFoundSkl [],
	avBranches [],
	avJunctions [],
	avTips [],
	avTriplePs [],
	avQuadruplePs [],
	avJunctionVx [],
	avSlabVx [],
	avBranchesOfAll [],
	avJunctionsOfAll [],
	avTipsOfAll [],
	avTriplePsOfAll [],
	avQuadruplePsOfAll [],
	avJunctionVxOfAll [],
	avSlabVxOfAll [],
	avTreeLength [],
	avAvBranchLength [],
	avMaxBranchLength [],
	avLargestShortestPath [],
	avTreeLengthOfAll [],
	avAvBranchLengthOfAll [],
	avMaxBranchLengthOfAll [],
	avLargestShortestPathOfAll [];
	
	double avMovingVectorLengthBIN [],
	avMovingVectorLengthSemiBIN [],
	avOccupVolume [],
	avLostVolume [],
	avMotility [],
	avDeltaVolume [],
	avDeltaRI [],
	avNrOfExtensions [],
	avNrOfRetractions [],
	avNrOfRetractionsMoreThan1Px [],
	avNrOfExtensionsMoreThan1Px [];
	
	//noninitilaized timelapseParticle
	public TimelapseParticle(){}	
	
	public TimelapseParticle(ArrayList<ImPoint> points, Calibration cal, double projectedFrames, boolean skeletonize, double gaussSigmaXY, double gaussSigmaZ,
			int orWidth, int orHeight, int orSlices, int orTimes, boolean minimizeImages, boolean binarizeBeforeSkeletonization, ProgressDialog progressDialog){		
//		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");		
//		progressDialog.notifyMessage("Started particle generation " + df.format(new Date()),ProgressDialog.NOTIFICATION);
		
		final double calibration = cal.pixelWidth;
		final double voxelDepth = cal.pixelDepth;
		vxVolume = calibration * calibration * voxelDepth;
		final double timePerFrame = cal.frameInterval;
				
		final int nPoints = points.size();
		pointList = new double [nPoints][5];	//x,y,z,t,intensity
		
		//get min/max values
		tMin = Integer.MAX_VALUE;
		tMax = 0;
		xMin = Integer.MAX_VALUE;
		xMax = 0;
		yMin = Integer.MAX_VALUE;
		yMax = 0;
		zMin = Integer.MAX_VALUE;
		zMax = 0;
		
		for(int i = 0; i < nPoints; i++){
			pointList[i][0] = points.get(i).x*calibration;
			pointList[i][1] = points.get(i).y*calibration;
			pointList[i][2] = points.get(i).z*voxelDepth;
			pointList[i][3] = points.get(i).t*timePerFrame;	
			pointList[i][4] = points.get(i).intensity;	
			
			if(pointList[i][0]/calibration>xMax)	xMax = (int)Math.round(pointList[i][0]/calibration);
			if(pointList[i][0]/calibration<xMin)	xMin = (int)Math.round(pointList[i][0]/calibration);
			if(pointList[i][1]/calibration>yMax)	yMax = (int)Math.round(pointList[i][1]/calibration);
			if(pointList[i][1]/calibration<yMin)	yMin = (int)Math.round(pointList[i][1]/calibration);
			if(pointList[i][2]/voxelDepth>zMax)		zMax = (int)Math.round(pointList[i][2]/voxelDepth);
			if(pointList[i][2]/voxelDepth<zMin)		zMin = (int)Math.round(pointList[i][2]/voxelDepth);
			if(pointList[i][3]/timePerFrame>tMax)	tMax = (int)Math.round(pointList[i][3]/timePerFrame);
			if(pointList[i][3]/timePerFrame<tMin)	tMin = (int)Math.round(pointList[i][3]/timePerFrame);
		}
		points.clear();
		System.gc();
		
		times = 1+tMax-tMin;
		slices = 1+zMax-zMin;
		width = 1+xMax-xMin;
		height = 1+yMax-yMin;
		data = new double [width][height][slices][times];
		
//		progressDialog.notifyMessage("Step 1 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
		
		//Determine static variables
		double maxIntensity = 0.0, maxPossibIntensity = 0.0;
		{
			xC = new double [times];
			yC = new double [times];
			zC = new double [times];
			xCOM = new double [times];
			yCOM = new double [times];
			zCOM = new double [times];
			double COMsum [] = new double [times];
			xSpan = new double [times];	double minX [] = new double [times];	double maxX [] = new double [times];
			ySpan = new double [times];	double minY [] = new double [times];	double maxY [] = new double [times];
			zSpan = new double [times];	double minZ [] = new double [times];	double maxZ [] = new double [times];
			averageIntensity = new double [times];	
			minimumIntensity = new double [times];	
			maximumIntensity = new double [times];	
			sdIntensity = new double [times];	
			volume = new double [times];
			surface = new double [times];
			RI = new double [times];
			
			convexHullVolume = new double [times];
			convexHullSurface = new double [times];
			convexHullxC = new double [times];
			convexHullyC = new double [times];
			convexHullzC = new double [times];
			xPolarityVectorBIN = new double [times];
			yPolarityVectorBIN = new double [times];
			zPolarityVectorBIN = new double [times];
			polarityVectorLengthBIN = new double [times];
			polarityIndexBIN = new double [times];
			xPolarityVectorSemiBIN = new double [times];
			yPolarityVectorSemiBIN = new double [times];
			zPolarityVectorSemiBIN = new double [times];
			polarityVectorLengthSemiBIN = new double [times];
			polarityIndexSemiBIN = new double [times];
						
			for(int t = 0; t < times; t++){				
				xC [t] = 0.0;	yC [t] = 0.0;	zC [t] = 0.0;
				xCOM [t] = 0.0;	yCOM [t] = 0.0;	zCOM [t] = 0.0;
				COMsum [t] = 0.0;
				minX [t] = Double.POSITIVE_INFINITY;
				maxX [t] = 0.0;
				minY [t] = Double.POSITIVE_INFINITY;
				maxY [t] = 0.0;
				minZ [t] = Double.POSITIVE_INFINITY;
				maxZ [t] = 0.0;	
				averageIntensity [t] = 0.0;	
				minimumIntensity [t] = Double.POSITIVE_INFINITY;	
				maximumIntensity [t] = 0.0;	
				sdIntensity [t] = 0.0;	
				volume [t] = 0.0;
				surface [t] = 0.0;
				
				convexHullVolume [t] = 0.0;
				convexHullSurface [t] = 0.0;
				convexHullxC [t] = 0.0;
				convexHullyC [t] = 0.0;
				convexHullzC [t] = 0.0;
			
				for(int z = 0; z < slices; z++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							data [x][y][z][t] = 0.0;	
						}
					}
				}
			}
			
//			progressDialog.notifyMessage("Step static 1 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			int xCal, yCal, zCal, tCal;
			for(int i = 0; i < nPoints; i++){
				tCal = (int)Math.round(pointList [i][3] / timePerFrame)-tMin;
				if(pointList[i][4] > maxIntensity){
					maxIntensity = pointList[i][4];
				}
				
				data [(int)Math.round(pointList [i][0]/calibration)-xMin][(int)Math.round(pointList [i][1]/calibration)-yMin][(int)Math.round(pointList [i][2]/voxelDepth)-zMin][tCal] = pointList[i][4];
				
				xC [tCal] += pointList[i][0];
				yC [tCal] += pointList[i][1];
				zC [tCal] += pointList[i][2];
				xCOM [tCal] += pointList[i][0] * pointList[i][4];
				yCOM [tCal] += pointList[i][1] * pointList[i][4];
				zCOM [tCal] += pointList[i][2] * pointList[i][4];
				COMsum [tCal] += pointList[i][4];
				
				if(pointList[i][0] > maxX [tCal]){	maxX [tCal] = pointList[i][0];}
				if(pointList[i][0] < minX [tCal]){	minX [tCal] = pointList[i][0];}
				if(pointList[i][1] > maxY [tCal]){	maxY [tCal] = pointList[i][1];}
				if(pointList[i][1] < minY [tCal]){	minY [tCal] = pointList[i][1];}
				if(pointList[i][2] > maxZ [tCal]){	maxZ [tCal] = pointList[i][2];}
				if(pointList[i][2] < minZ [tCal]){	minZ [tCal] = pointList[i][2];}
				
				averageIntensity [tCal] += pointList [i][4];	
				if(pointList[i][4] > maximumIntensity [tCal]){
					maximumIntensity [tCal] = pointList[i][4];
				}
				if(pointList[i][4] < minimumIntensity [tCal]){
					minimumIntensity [tCal] = pointList[i][4];
				}				
				
				volume [tCal] += vxVolume;
			}
			
//			progressDialog.notifyMessage("Step static 2 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			for(int t = 0; t < times; t++){		
				averageIntensity [t] /= (volume [t] / vxVolume);
			}
						
			for(int i = 0; i < nPoints; i++){				
				xCal = (int)Math.round(pointList [i][0] / calibration);
				yCal = (int)Math.round(pointList [i][1] / calibration);
				zCal = (int)Math.round(pointList [i][2] / voxelDepth);
				tCal = (int)Math.round(pointList [i][3] / timePerFrame)-tMin;
				
				sdIntensity [tCal] += Math.pow(pointList [i][4] - averageIntensity [tCal], 2.0);			
				
				//Look for other cell particles in surroundings
				double transientSurface = 0.0;
				if(yCal-1>=yMin){ 
					if(data[xCal-xMin][yCal-1-yMin][zCal-zMin][tCal]==0.0){transientSurface += calibration*voxelDepth;}
				}else{transientSurface += calibration*voxelDepth;}
				if(yCal+1<=yMax){ 
					if(data[xCal-xMin][yCal+1-yMin][zCal-zMin][tCal]==0.0){transientSurface += calibration*voxelDepth;}
				}else{transientSurface += calibration*voxelDepth;}
				if(xCal-1>=xMin){ 
					if(data[xCal-1-xMin][yCal-yMin][zCal-zMin][tCal]==0.0){transientSurface += calibration*voxelDepth;}
				}else{transientSurface += calibration*voxelDepth;}
				if(xCal+1<=xMax){ 
					if(data[xCal+1-xMin][yCal-yMin][zCal-zMin][tCal]==0.0){transientSurface += calibration*voxelDepth;}
				}else{transientSurface += calibration*voxelDepth;}
				if(zCal-1>=zMin){ 
					if(data[xCal-xMin][yCal-yMin][zCal-1-zMin][tCal]==0.0){transientSurface += calibration*calibration;}
				}else{transientSurface += calibration*calibration;}
				if(zCal+1<=zMax){ 
					if(data[xCal-xMin][yCal-yMin][zCal+1-zMin][tCal]==0.0){transientSurface += calibration*calibration;}
				}else{transientSurface += calibration*calibration;}
				//Look for other cell particles in surroundings
				
				surface [tCal] += transientSurface;
			}
			
//			progressDialog.notifyMessage("Step static 3 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			double transientRadius;
			for(int t = 0; t < times; t++){
				xC [t] /= (volume [t]/vxVolume);
				yC [t] /= (volume [t]/vxVolume);
				zC [t] /= (volume [t]/vxVolume);
				xCOM [t] /= COMsum [t];
				yCOM [t] /= COMsum [t];
				zCOM [t] /= COMsum [t];
				xC [t] += calibration / 2.0;
				yC [t] += calibration / 2.0;
				zC [t] += voxelDepth / 2.0;
				xCOM [t] += calibration / 2.0;
				yCOM [t] += calibration / 2.0;
				zCOM [t] += voxelDepth / 2.0;
				xSpan [t] = maxX [t] - minX [t] + calibration;
				ySpan [t] = maxY [t] - minY [t] + calibration;
				zSpan [t] = maxZ [t] - minZ [t] + voxelDepth;
				sdIntensity [t] /= (volume [t] / vxVolume)-1.0;
				sdIntensity [t] = Math.sqrt(sdIntensity[t]);
				
		   		transientRadius = Math.pow((double)((volume [t]*3.0)/(4.0*Math.PI)), (double)1/3.0);			
				RI [t] =  surface [t] / (Math.PI * Math.pow(transientRadius,2) * 4);	//RI
			}			
			
//			progressDialog.notifyMessage("Step static 4 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			{
				//convex hull calculation
				hullData = new boolean [width][height][slices][times];
				for(int t = 0; t < times; t++){
					boolean [][][] transHullData = new boolean [width][height][slices];
					for(int z = 0; z < slices; z++){
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){
								if(data [x][y][z][t] > 0.0){
									transHullData [x][y][z] = true;
								}else{
									transHullData [x][y][z] = false;
								}
									
							}
						}
					}
					
					//calculate hull
					transHullData = getConv3DHull(transHullData, progressDialog);
					
					for(int z = 0; z < slices; z++){
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){
								hullData [x][y][z][t] = transHullData [x][y][z];									
							}
						}
					}
				}
			}
			
//			progressDialog.notifyMessage("Step static 5 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			{
				String bitDepth = "8-bit"; maxPossibIntensity = 255.0;
				if(maxIntensity>255.0){bitDepth = "16-bit"; maxPossibIntensity = 65535.0;}
				if(maxIntensity>65535.0){bitDepth = "32-bit"; maxPossibIntensity = 2147483647.0;}
							
				if(minimizeImages){	
					//(java.lang.String title,java.lang.String type,int width,int height,int channels,int slices,int frames)
					particleImp = IJ.createImage("Particle image", bitDepth, width+4, height+4, 1, slices, times);
					convexHullImp = IJ.createImage("Convex Hull Image", "8-bit", width+4, height+4, 1, slices, times);
					for(int t = 0; t < times; t++){
						for(int z = 0; z < slices; z++){
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									int iz = particleImp.getStackIndex(1,z+1,t+1)-1;	//(int channel,int slice,int frame);
																	
									if(hullData [x][y][z][t]){
										//Convex hull volume data
										convexHullVolume [t] += vxVolume;
										convexHullxC [t] += (double)(x+xMin);
										convexHullyC [t] += (double)(y+yMin);	
										convexHullzC [t] += (double)(z+zMin);	
										
										//Convex hull surface data									
										double transientSurface = 0.0;
										//Look for other cell particles in surroundings
										if(y-1>=0){ 
											if(!hullData[x][y-1][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(y+1<height){ 
											if(!hullData[x][y+1][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(x-1>=0){ 
											if(!hullData[x-1][y][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(x+1<width){ 
											if(!hullData[x+1][y][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(z-1>=0){ 
											if(!hullData[x][y][z-1][t]){transientSurface += calibration*calibration;}
										}else{transientSurface += calibration*calibration;}
										if(z+1<slices){ 
											if(!hullData[x][y][z+1][t]){transientSurface += calibration*calibration;}
										}else{transientSurface += calibration*calibration;}
										//Look for other cell particles in surroundings
										convexHullSurface [t] += transientSurface;									
											
										if(transientSurface!=0.0){										
											convexHullImp.getStack().setVoxel(x+2,y+2,iz,255.0);
										}else{
											convexHullImp.getStack().setVoxel(x+2,y+2,iz,0.0);
										}
									}else{
										convexHullImp.getStack().setVoxel(x+2,y+2,iz,0.0);
									}
		
									//generate particle image
									if(data[x][y][z][t] != 0.0){
										particleImp.getStack().setVoxel(x+2,y+2,iz,data[x][y][z][t]);
									}else{
										particleImp.getStack().setVoxel(x+2,y+2,iz,0.0);
									}
								}
							}
						}
					}
					particleImp.setCalibration(cal);
					particleImp.setDisplayRange(0.0,maxIntensity);
					convexHullImp.setCalibration(cal);	
				}else{
					//(java.lang.String title,java.lang.String type,int width,int height,int channels,int slices,int frames)
					particleImp = IJ.createImage("Particle image", bitDepth, orWidth, orHeight, 1, orSlices, orTimes);
					particleImp.setCalibration(cal);
					convexHullImp = IJ.createImage("Convex Hull Image", "8-bit", orWidth, orHeight, 1, orSlices, orTimes);
					for(int t = 0; t < times; t++){
						for(int z = 0; z < slices; z++){
							for(int x = 0; x < width; x++){
								for(int y = 0; y < height; y++){
									int iz = particleImp.getStackIndex(1,z+zMin+1,t+tMin+1)-1;	//(int channel,int slice,int frame);
									
									if(hullData [x][y][z][t]){
										//Convex hull volume data
										convexHullVolume [t] += vxVolume;
										convexHullxC [t] += (double)(x+xMin);
										convexHullyC [t] += (double)(y+yMin);	
										convexHullzC [t] += (double)(z+zMin);	
									
										//Convex hull surface data
										double transientSurface = 0.0;
										//Look for other cell particles in surroundings
										if(y-1>=0){ 
											if(!hullData[x][y-1][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(y+1<height){ 
											if(!hullData[x][y+1][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(x-1>=0){ 
											if(!hullData[x-1][y][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(x+1<width){ 
											if(!hullData[x+1][y][z][t]){transientSurface += calibration*voxelDepth;}
										}else{transientSurface += calibration*voxelDepth;}
										if(z-1>=0){ 
											if(!hullData[x][y][z-1][t]){transientSurface += calibration*calibration;}
										}else{transientSurface += calibration*calibration;}
										if(z+1<slices){ 
											if(!hullData[x][y][z+1][t]){transientSurface += calibration*calibration;}
										}else{transientSurface += calibration*calibration;}
										//Look for other cell particles in surroundings
										convexHullSurface [t] += transientSurface;
										
										if(transientSurface!=0.0){
											convexHullImp.getStack().setVoxel(x+xMin,y+yMin,iz,255.0);
										}else{
											convexHullImp.getStack().setVoxel(x+xMin,y+yMin,iz,0.0);
										}
									}else{
										convexHullImp.getStack().setVoxel(x+xMin,y+yMin,iz,0.0);
									}
									
									//generate particle image
									if(data[x][y][z][t] != 0.0){
										particleImp.getStack().setVoxel(x+xMin,y+yMin,iz,data[x][y][z][t]);
									}else{
										particleImp.getStack().setVoxel(x+xMin,y+yMin,iz,0.0);
									}
								}
							}
						}					
					}
				}	
				particleImp.setCalibration(cal);
				particleImp.setDisplayRange(0.0,maxIntensity);
				convexHullImp.setCalibration(cal);				
			}			
			
//			progressDialog.notifyMessage("Step static 6 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			for(int t = 0; t < times; t++){
				convexHullxC [t] /= (convexHullVolume [t]/vxVolume);
				convexHullyC [t] /= (convexHullVolume [t]/vxVolume);
				convexHullzC [t] /= (convexHullVolume [t]/vxVolume);
				convexHullxC [t] *= calibration;
				convexHullyC [t] *= calibration;
				convexHullzC [t] *= voxelDepth;
				convexHullxC [t] += calibration / 2.0;
				convexHullyC [t] += calibration / 2.0;
				convexHullzC [t] += voxelDepth / 2.0;
				
				xPolarityVectorBIN [t] = convexHullxC [t] - xC [t];
				yPolarityVectorBIN [t] = convexHullyC [t] - yC [t];
				zPolarityVectorBIN [t] = convexHullzC [t] - zC [t];
				polarityVectorLengthBIN [t] = Math.sqrt(Math.pow(xPolarityVectorBIN [t],2.0)+Math.pow(yPolarityVectorBIN [t],2.0)+Math.pow(zPolarityVectorBIN [t],2.0));
				polarityIndexBIN [t] = polarityVectorLengthBIN [t]/(2*Math.pow((double)((convexHullVolume [t]*3)/(4*Math.PI)), (double)1/3));
				
				xPolarityVectorSemiBIN [t] = convexHullxC [t] - xCOM [t];
				yPolarityVectorSemiBIN [t] = convexHullyC [t] - yCOM [t];
				zPolarityVectorSemiBIN [t] = convexHullzC [t] - zCOM [t];
				polarityVectorLengthSemiBIN [t] = Math.sqrt(Math.pow(xPolarityVectorSemiBIN [t],2.0)+Math.pow(yPolarityVectorSemiBIN [t],2.0)+Math.pow(zPolarityVectorSemiBIN [t],2.0));
				polarityIndexSemiBIN [t] = polarityVectorLengthSemiBIN [t]/(2*Math.pow((double)((convexHullVolume [t]*3)/(4*Math.PI)), (double)1/3));
			}
		}
		
//		progressDialog.notifyMessage("Step 2 SKL " + df.format(new Date()),ProgressDialog.NOTIFICATION);
		
		// SKELETON PARAMETERS
		if(skeletonize){
			IDofLargest = new int [times];
			foundSkl = new int [times];
			branches = new int [times];
			junctions = new int [times];
			tips = new int [times];
			triplePs = new int [times];
			quadruplePs = new int [times];
			junctionVx = new int [times];
			slabVx = new int [times];
			treeLength = new double [times];
			avBranchLength = new double [times];
			maxBranchLength = new double [times];
			largestShortestPath = new double [times];
			
			branchesOfAll = new int [times];
			junctionsOfAll = new int [times];
			tipsOfAll = new int [times];
			triplePsOfAll = new int [times];
			quadruplePsOfAll = new int [times];
			junctionVxOfAll = new int [times];
			slabVxOfAll = new int [times];		
			treeLengthOfAll = new double [times];
			avBranchLengthOfAll = new double [times];
			maxBranchLengthOfAll = new double [times];
			largestShortestPathOfAll = new double [times];
				
			skeletonImp = IJ.createHyperStack("Skl image", particleImp.getWidth(), particleImp.getHeight(), particleImp.getNChannels(),
					particleImp.getNSlices(), particleImp.getNFrames(), 8);	
			skeletonImp.setCalibration(cal);	
			
			ImagePlus particleImpGaussed = particleImp.duplicate();
			particleImpGaussed.setCalibration(cal);
			
			if(binarizeBeforeSkeletonization){
				for(int t = 0; t < particleImp.getNFrames(); t++){
					for(int z = 0; z < particleImp.getNSlices(); z++){
						for(int x = 0; x < particleImp.getWidth(); x++){
							for(int y = 0; y < particleImp.getHeight(); y++){
								int iz = 0;
//								if(minimizeImages){
									iz = particleImp.getStackIndex(1,z+1,t+1)-1;	//(int channel,int slice,int frame);
//								}else{
//									iz = particleImp.getStackIndex(1,z+zMin+1,t+tMin+1)-1;	//(int channel,int slice,int frame);
//								}
								if(particleImpGaussed.getStack().getVoxel(x,y,iz)>0.0){
									particleImpGaussed.getStack().setVoxel(x,y,iz,maxPossibIntensity);
								}
							}
						}
					}					
				}
			}
			
			//make 8-bit to fit requirements for use of skeletonize plug-in
			particleImpGaussed.setDisplayRange(0.0,maxIntensity);
			IJ.run(particleImpGaussed, "8-bit", "");
			
			//Gaussfilter
				DecimalFormat gaussformat = new DecimalFormat("#0.0");
				gaussformat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
				IJ.run(particleImpGaussed, "Gaussian Blur 3D...", "x=" + gaussformat.format(gaussSigmaXY) + " y=" + gaussformat.format(gaussSigmaXY) + " z=" + gaussformat.format(gaussSigmaZ));				
			// Gaussfilter
							
			ImagePlus tempImp = IJ.createHyperStack("Skl image", particleImp.getWidth(), particleImp.getHeight(), 1, particleImp.getNSlices(), 1, 8);	;
			ImageStack tempStack; 
			int iz;
			ArrayList<Double> lst;
			
			for(int t = 0; t < times; t++){
				tempStack = new ImageStack(particleImpGaussed.getWidth(),particleImpGaussed.getHeight());
				for(int z = 0; z < particleImp.getNSlices(); z++){
					particleImpGaussed.setZ(z+1);
					particleImpGaussed.setT(t+1);
					tempStack.addSlice(particleImpGaussed.getProcessor().duplicate());
				}
				tempImp = new ImagePlus();
				tempImp.setStack(tempStack);
				tempImp.setCalibration(cal);

//				IJ.run(tempImp,"Skeletonize (2D/3D)","");
				Skeletonize3D_ skelProc = new Skeletonize3D_();
				skelProc.setup("", tempImp);
				skelProc.run(tempImp.getProcessor());
								
				//	Before skeleton analysis: cut skeleton in empty space	//TODO change for future mode
//				tempImp = pruneSkeleton(particleImp, tempImp);
				
				AnalyzeSkeleton_ skel = new AnalyzeSkeleton_();
				skel.calculateShortestPath = true;
				skel.setup("", tempImp);
				
				SkeletonResult sklRes = skel.run(AnalyzeSkeleton_.NONE, false, true, null, true, false);
				//run(int pruneIndex, boolean pruneEnds, boolean shortPath, ImagePlus origIP, boolean silent, boolean verbose)
				
				{
					ImagePlus sklResultImage = new ImagePlus("skeleton " + (t+1) + "", skel.getResultImage(false));
					for(int x = 0; x < sklResultImage.getWidth(); x++){
						for(int y = 0; y < sklResultImage.getHeight(); y++){
							for(int z = 0; z < sklResultImage.getNSlices(); z++){
								iz = particleImp.getStackIndex(1,z+1,t+1)-1;	//(int channel,int slice,int frame);
								skeletonImp.getStack().setVoxel(x,y,iz,sklResultImage.getStack().getVoxel(x,y,z));
							}
						}
					}
					sklResultImage.changes = false;
					sklResultImage.close();
				}
				
								
				foundSkl [t] = sklRes.getNumOfTrees();				
				IDofLargest [t] = 0;	double sklLargestValue = 0.0;
				branchesOfAll [t] = 0;
				junctionsOfAll [t] = 0;
				tipsOfAll [t] = 0;
				triplePsOfAll [t] = 0;
				quadruplePsOfAll [t] = 0;
				junctionVxOfAll [t] = 0;
				slabVxOfAll [t] = 0;				
				int averageOfAllCounter = 0;
				treeLengthOfAll [t] = 0.0;
				maxBranchLengthOfAll [t] = 0.0;
				avBranchLengthOfAll [t] = 0.0;
				largestShortestPathOfAll [t] = 0.0;
				
				if(foundSkl [t]>0){
					int [] sBranches = sklRes.getBranches();
					int [] sJunctions = sklRes.getJunctions();
					int [] sTips = sklRes.getEndPoints();
					int [] sJunctionVx = sklRes.getJunctionVoxels();
					int [] sSlabs = sklRes.getSlabs();
					int [] sTriples = sklRes.getTriples();
					int [] sQuadruples = sklRes.getQuadruples();
					double [] sAvBrL = sklRes.getAverageBranchLength();
					double [] sMaxBrL = sklRes.getMaximumBranchLength();
					double [] sTreeL = new double [foundSkl [t]];
					lst = sklRes.getShortestPathList();
//					IJ.log(lst.size() + " - size");
					
					for(int i = 0; i < foundSkl [t]; i++){
						sTreeL [i] = sAvBrL[i]*sBranches[i];	//Total Tree length
						if(sTreeL [i] > sklLargestValue){
							IDofLargest [t] = i; sklLargestValue = sTreeL [i] ;
						}
						
						//Parameters for connecting all skeletons
						if(sBranches[i] > 0){	//connect only if at least one branch
							if(i>0){
								branchesOfAll [t] += sBranches[i] - 1;
								tipsOfAll [t] += sTips[i] - 2;
								if(sMaxBrL[i] > maxBranchLengthOfAll [t]){
									maxBranchLengthOfAll [t] = 0 + sMaxBrL[i];
								}
							}else{
								branchesOfAll [t] += sBranches[i];
								tipsOfAll [t] += sTips[i];
								maxBranchLengthOfAll [t] = 0 + sMaxBrL[i];
							}
							junctionsOfAll [t] += sJunctions[i];
							junctionVxOfAll [t] += sJunctionVx[i];
							slabVxOfAll [t] += sSlabs [i];						
							avBranchLengthOfAll [t] += (sAvBrL[i] * sBranches[i]);
								averageOfAllCounter += sBranches[i];
							triplePsOfAll [t] += sTriples [i];
							quadruplePsOfAll [t] += sQuadruples [i];
							treeLengthOfAll [t] += sTreeL[i];
							largestShortestPathOfAll [t] += lst.get(i); 
						}						
					}
					avBranchLengthOfAll [t] /= (double) averageOfAllCounter;
					
					//Get values of largest Skeleton
					branches [t] = sBranches [IDofLargest[t]];	//# Branches
					junctions [t] = sJunctions [IDofLargest[t]];	//# Junctions
					tips [t] = sTips [IDofLargest[t]];	//# End-Points
					junctionVx [t] = sJunctionVx [IDofLargest[t]];	//# Junction Voxels
					slabVx [t] = sSlabs [IDofLargest[t]];	//# Slab Voxels
					avBranchLength [t] = sAvBrL [IDofLargest[t]];	//Averaged Branch length
					triplePs [t] = sTriples [IDofLargest[t]];	//# Triple Points
					quadruplePs [t] = sQuadruples [IDofLargest[t]];	//# Quadruple Points
					maxBranchLength [t] = sMaxBrL [IDofLargest[t]];	//Maximum Branch length
					treeLength [t] = sTreeL [IDofLargest[t]];	//Total Tree length
					largestShortestPath [t] = lst.get(IDofLargest[t]);
					lst.clear();
					lst.trimToSize();
				}				
			}
			
			tempImp.changes = false;
			tempImp.close();
			particleImpGaussed.changes = false;
			particleImpGaussed.close();
			System.gc();			
		}
				
//		progressDialog.notifyMessage("Step 3 TIMEDEP " + df.format(new Date()),ProgressDialog.NOTIFICATION);
		
		//TIME DEPENDENT VARIABLES
		if(times>1){
			deltaVolume = new double [times];
			deltaRI = new double [times];
			movingVectorLengthBIN = new double [times];
			movingVectorLengthSemiBIN = new double [times];
			occupVolume = new double [times];
			lostVolume = new double [times];
			nrOfExtensions = new int [times];
			nrOfRetractions = new int [times];
			nrOfExtensionsMoreThan1Vx = new int [times];
			nrOfRetractionsMoreThan1Vx = new int [times];
			boolean extensRetractions [][][][][] = new boolean [width][height][slices][times][2];	//0 = ext, 1 = retr
			motility = new double [times];
						
			deltaVolume [0] = 0.0;
			deltaRI [0] = 0.0;
			movingVectorLengthBIN [0] = 0.0;
			movingVectorLengthSemiBIN [0] = 0.0;
			occupVolume [0] = 0.0;
			lostVolume [0] = 0.0;
			nrOfExtensions [0] = 0;
			nrOfRetractions [0] = 0;
			nrOfExtensionsMoreThan1Vx [0] = 0;
			nrOfRetractionsMoreThan1Vx [0] = 0;
			motility [0] = 0.0;
					
			for(int t = 1; t < times; t++){
				deltaVolume [t] = volume [t] - volume [t-1];
				deltaRI [t] = RI [t] - RI [t-1];
				movingVectorLengthBIN [t] = Math.sqrt(Math.pow(xC [t] - xC [t-1],2.0)
						+ Math.pow(yC [t] - yC [t-1], 2.0)
						+ Math.pow(zC [t] - zC [t-1], 2.0));
				movingVectorLengthSemiBIN [t] = Math.sqrt(Math.pow(xCOM [t] - xCOM [t-1],2.0)
						+ Math.pow(yCOM [t] - yCOM [t-1], 2.0)
						+ Math.pow(zCOM [t] - zCOM [t-1], 2.0));
				occupVolume[t] = 0.0;
				lostVolume[t] = 0.0;
				for(int z = 0; z < slices; z++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){							
							if(data [x][y][z][t] > 0.0 && data [x][y][z][t-1] == 0.0){
								occupVolume [t] += vxVolume;
								extensRetractions [x][y][z][t][0] = true;
							}else{
								extensRetractions [x][y][z][t][0] = false;
							}
							if(data [x][y][z][t] == 0.0 && data [x][y][z][t-1] > 0.0){
								lostVolume [t] += vxVolume;
								extensRetractions [x][y][z][t][1] = true;
							}else{
								extensRetractions [x][y][z][t][1] = false;
							}
						}
						
					}
				}
				motility [t] = occupVolume [t] + lostVolume [t];
			}	
			
//			progressDialog.notifyMessage("Step 3.1 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			//FloodFiller to find #extensions and #retractions
			int floodNodeX, floodNodeY, floodNodeZ, floodNodeT, index, maxExtRetr = 0;
			for(int t = 1; t < times; t++){
				if(maxExtRetr < (int)Math.round(occupVolume [t] / vxVolume)){
					maxExtRetr = (int)Math.round(occupVolume [t] / vxVolume);
				}
				if(maxExtRetr < (int)Math.round(lostVolume [t] / vxVolume)){
					maxExtRetr = (int)Math.round(lostVolume [t] / vxVolume);
				}				
			}
			int[][] floodNodes = new int[maxExtRetr][4];
			int extensionsCounter, retractionsCounter;
			for(int t = 1; t < times; t++){				
				System.gc();
				for(int z = 0; z < slices; z++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							//extensions flood-filler
							if(extensRetractions[x][y][z][t][0]){
								extensionsCounter = 1;
								extensRetractions[x][y][z][t][0] = false;
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
									if ((floodNodeX > 0) && (extensRetractions[floodNodeX-1][floodNodeY][floodNodeZ][floodNodeT][0])){
										extensRetractions[floodNodeX-1][floodNodeY][floodNodeZ][floodNodeT][0] = false;
										
										extensionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX-1;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}
									if ((floodNodeX < (width-1)) && (extensRetractions[floodNodeX+1][floodNodeY][floodNodeZ][floodNodeT][0])){
										extensRetractions[floodNodeX+1][floodNodeY][floodNodeZ][floodNodeT][0] = false;
										extensionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX+1;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}
									if ((floodNodeY > 0) && (extensRetractions[floodNodeX][floodNodeY-1][floodNodeZ][floodNodeT][0])){
										extensRetractions[floodNodeX][floodNodeY-1][floodNodeZ][floodNodeT][0] = false;
										extensionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY-1;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}                
									if ((floodNodeY < (height-1)) && (extensRetractions[floodNodeX][floodNodeY+1][floodNodeZ][floodNodeT][0])){
										extensRetractions[floodNodeX][floodNodeY+1][floodNodeZ][floodNodeT][0] = false;
										extensionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY+1;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}
									if ((floodNodeZ > 0) && (extensRetractions[floodNodeX][floodNodeY][floodNodeZ-1][floodNodeT][0])){
										extensRetractions[floodNodeX][floodNodeY][floodNodeZ-1][floodNodeT][0] = false;
										extensionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ-1;
										floodNodes[index][3] = floodNodeT;
									}                
									if ((floodNodeZ < (slices-1)) && (extensRetractions[floodNodeX][floodNodeY][floodNodeZ+1][floodNodeT][0])){
										extensRetractions[floodNodeX][floodNodeY][floodNodeZ+1][floodNodeT][0] = false;
										extensionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ+1;
										floodNodes[index][3] = floodNodeT;
									}
								}								
								//Filter and save
								nrOfExtensions [t]++;
								if(extensionsCounter > 1){
									nrOfExtensionsMoreThan1Vx [t]++;
								}							
							}
							
							//retractions flood-filler
							if(extensRetractions[x][y][z][t][1]){
								retractionsCounter = 1;
								extensRetractions[x][y][z][t][1] = false;
								
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
									if ((floodNodeX > 0) && (extensRetractions[floodNodeX-1][floodNodeY][floodNodeZ][floodNodeT][1])){
										extensRetractions[floodNodeX-1][floodNodeY][floodNodeZ][floodNodeT][1] = false;
										
										retractionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX-1;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}
									if ((floodNodeX < (width-1)) && (extensRetractions[floodNodeX+1][floodNodeY][floodNodeZ][floodNodeT][1])){
										extensRetractions[floodNodeX+1][floodNodeY][floodNodeZ][floodNodeT][1] = false;
										retractionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX+1;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}
									if ((floodNodeY > 0) && (extensRetractions[floodNodeX][floodNodeY-1][floodNodeZ][floodNodeT][1])){
										extensRetractions[floodNodeX][floodNodeY-1][floodNodeZ][floodNodeT][1] = false;
										retractionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY-1;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}                
									if ((floodNodeY < (height-1)) && (extensRetractions[floodNodeX][floodNodeY+1][floodNodeZ][floodNodeT][1])){
										extensRetractions[floodNodeX][floodNodeY+1][floodNodeZ][floodNodeT][1] = false;
										retractionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY+1;
										floodNodes[index][2] = floodNodeZ;
										floodNodes[index][3] = floodNodeT;
									}
									if ((floodNodeZ > 0) && (extensRetractions[floodNodeX][floodNodeY][floodNodeZ-1][floodNodeT][1])){
										extensRetractions[floodNodeX][floodNodeY][floodNodeZ-1][floodNodeT][1] = false;
										retractionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ-1;
										floodNodes[index][3] = floodNodeT;
									}                
									if ((floodNodeZ < (slices-1)) && (extensRetractions[floodNodeX][floodNodeY][floodNodeZ+1][floodNodeT][1])){
										extensRetractions[floodNodeX][floodNodeY][floodNodeZ+1][floodNodeT][1] = false;
										retractionsCounter++;
										
										index++;
										floodNodes[index][0] = floodNodeX;
										floodNodes[index][1] = floodNodeY;
										floodNodes[index][2] = floodNodeZ+1;
										floodNodes[index][3] = floodNodeT;
									}
								}								
								//Filter and save
								nrOfRetractions [t]++;
								if(retractionsCounter > 1){
									nrOfRetractionsMoreThan1Vx [t]++;
								}							
							}
						}
					}
				}				
			}				
		}
		
//		progressDialog.notifyMessage("Step 4 LTPs " + df.format(new Date()),ProgressDialog.NOTIFICATION);
		
		//timegroup variables
		projectedTimes = (int)((double)times/(double)projectedFrames);
		boolean particleTouched,
				hullTouched,
				particleStatic,
				hullStatic;
		int group;
		if(times>=projectedFrames&&projectedFrames!=1){
			projectedVolume = new double [projectedTimes];
			projectedStaticVolume = new double [projectedTimes];
			projectedDynamicFraction = new double [projectedTimes];
			projectedConvexHullVolume = new double [projectedTimes];
			projectedConvexHullStaticVolume = new double [projectedTimes];
			projectedConvexHullDynamicFraction = new double [projectedTimes];
			
			for(int i = 0; i < projectedTimes; i++){
				projectedVolume [i] = 0.0;
				projectedStaticVolume [i] = 0.0;
				projectedConvexHullVolume [i] = 0.0;
				projectedConvexHullStaticVolume [i] = 0.0;				
			}
			
			for(int z = 0; z < slices; z++){
				for(int x = 0; x < width; x++){
					for(int y = 0; y < height; y++){
						particleTouched = false;
						hullTouched = false;
						particleStatic = true;
						hullStatic = true;
						for(int t = 0; t < times; t++){
							if(data [x][y][z][t] > 0.0){
								particleTouched = true;
							}else{
								particleStatic = false;
							}
							if(hullData [x][y][z][t]){
								hullTouched = true;
							}else{
								hullStatic = false;
							}
							
							//Save into group
							if((t+1)%projectedFrames==0){		
								group = ((int)((double)t/projectedFrames));
								if(particleTouched == true){
									projectedVolume [group] += vxVolume;
									particleTouched = false;
								}
								if(hullTouched == true){
									projectedConvexHullVolume [group] += vxVolume;
									hullTouched = false;
								}
								if(particleStatic == true){
									projectedStaticVolume [group] += vxVolume;
								}else{
									particleStatic = true;
								}
								if(hullStatic == true){
									projectedConvexHullStaticVolume [group] += vxVolume;
								}else{
									hullStatic = true;
								}
							}
						}						
					}// end for(x)
				}
			}
			
//			progressDialog.notifyMessage("Step 4 LTPs 1 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			for(int i = 0; i < projectedTimes; i++){
				projectedDynamicFraction [i] = (projectedVolume [i] - projectedStaticVolume [i]) / projectedVolume [i];
				projectedConvexHullDynamicFraction [i] = (projectedConvexHullVolume [i] - projectedConvexHullStaticVolume [i]) / projectedConvexHullVolume [i];
			}
			
			//Directionality Parameters
			accumulatedDistanceBIN = new double [projectedTimes];
			euclideanDistanceBIN = new double [projectedTimes];
			directionalityIndexBIN = new double [projectedTimes];			
			accumulatedDistanceSemiBIN = new double [projectedTimes];
			euclideanDistanceSemiBIN = new double [projectedTimes];
			directionalityIndexSemiBIN = new double [projectedTimes];
			
			double accumDistBIN = 0.0, 
					accumDistSemiBIN = 0.0, 
					x0BIN = xC[0], 
					y0BIN = yC[0], 
					z0BIN = zC[0], 
					x0SemiBIN = xCOM[0], 
					y0SemiBIN = yCOM [0],
					z0SemiBIN = zCOM [0];
					
			for(int t = 1; t < times; t++){
				accumDistBIN += movingVectorLengthBIN [t];
				accumDistSemiBIN += movingVectorLengthSemiBIN [t];
								
				if((t+1)%projectedFrames==0){
					group = ((int)((double)t/projectedFrames));
					accumulatedDistanceBIN [group] = accumDistBIN;
					euclideanDistanceBIN [group] = Math.sqrt(Math.pow(x0BIN - xC [t], 2.0)
							+ Math.pow(y0BIN - yC [t], 2.0)
							+ Math.pow(z0BIN - zC [t], 2.0));
					directionalityIndexBIN [group] = euclideanDistanceBIN [group]/ accumulatedDistanceBIN [group];
					accumulatedDistanceSemiBIN [group] = accumDistSemiBIN;
					euclideanDistanceSemiBIN [group] = Math.sqrt(Math.pow(x0SemiBIN - xCOM [t], 2.0)
							+ Math.pow(y0SemiBIN - yCOM [t], 2.0)
							+ Math.pow(z0SemiBIN - zCOM [t], 2.0));
					directionalityIndexSemiBIN [group] = euclideanDistanceSemiBIN [group]/ accumulatedDistanceSemiBIN [group];
											
					accumDistBIN = 0.0;	
					accumDistSemiBIN = 0.0;
					if(t+1<times){
						x0BIN = xC[t+1]; 
						y0BIN = yC[t+1];
						z0BIN = zC[t+1];
						x0SemiBIN = xCOM[t+1];
						y0SemiBIN = yCOM [t+1];
						z0SemiBIN = zCOM [t+1];
					}
				}
			}
		}
		
//		progressDialog.notifyMessage("Step 4 LTPs 2 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
		
		//LONG TERM AVERAGES OF PARAMETERS
		if(times>=projectedFrames&&projectedFrames!=1){
			avXC = new double [projectedTimes];
			avYC = new double [projectedTimes];
			avZC = new double [projectedTimes];
			avXCOM = new double [projectedTimes];
			avYCOM = new double [projectedTimes];
			avZCOM = new double [projectedTimes];
			avXSpan = new double [projectedTimes];
			avYSpan = new double [projectedTimes];
			avZSpan = new double [projectedTimes];
			avAverageIntensity = new double [projectedTimes];
			avMinimumIntensity = new double [projectedTimes];
			avMaximumIntensity = new double [projectedTimes];
			avSdIntensity = new double [projectedTimes];
			avVolume = new double [projectedTimes]; 
			avSurface = new double [projectedTimes];
			avRI = new double [projectedTimes];
			avConvexHullVolume = new double [projectedTimes];
			avConvexHullSurface = new double [projectedTimes];
			avConvexHullxC = new double [projectedTimes];
			avConvexHullyC = new double [projectedTimes];
			avConvexHullzC = new double [projectedTimes];
			avXPolarityVectorBIN = new double [projectedTimes];
			avYPolarityVectorBIN = new double [projectedTimes];
			avZPolarityVectorBIN = new double [projectedTimes];
			avPolarityVectorLengthBIN = new double [projectedTimes];
			avPolarityIndexBIN = new double [projectedTimes]; 
			avXPolarityVectorSemiBIN = new double [projectedTimes];
			avYPolarityVectorSemiBIN = new double [projectedTimes];
			avZPolarityVectorSemiBIN = new double [projectedTimes];
			avPolarityVectorLengthSemiBIN = new double [projectedTimes];
			avPolarityIndexSemiBIN = new double [projectedTimes];
			
			for(int t = 0; t < times-times%projectedFrames; t++){
				group = ((int)((double)t/projectedFrames));
				avXC [group] += xC [t];
				avYC [group] += yC [t]; 
				avZC [group] += zC [t]; 
				avXCOM [group] += xCOM [t]; 
				avYCOM [group] += yCOM [t];
				avZCOM [group] += zCOM [t];
				avXSpan [group] += xSpan [t]; 
				avYSpan [group] += ySpan [t];
				avZSpan [group] += zSpan [t];
				avAverageIntensity [group] += averageIntensity [t]; 
				avMinimumIntensity [group] += minimumIntensity [t]; 
				avMaximumIntensity [group] += maximumIntensity [t]; 
				avSdIntensity [group] += sdIntensity [t]; 
				avVolume [group] += volume [t]; 
				avSurface [group] += surface [t]; 
				avRI [group] += RI [t]; 
				avConvexHullVolume [group] += convexHullVolume [t]; 
				avConvexHullSurface [group] += convexHullSurface [t]; 
				avConvexHullxC [group] += convexHullxC [t]; 
				avConvexHullyC [group] += convexHullyC [t]; 
				avConvexHullzC [group] += convexHullzC [t]; 
				avXPolarityVectorBIN [group] += xPolarityVectorBIN [t]; 
				avYPolarityVectorBIN [group] += yPolarityVectorBIN [t]; 
				avZPolarityVectorBIN [group] += zPolarityVectorBIN [t]; 
				avPolarityVectorLengthBIN [group] += polarityVectorLengthBIN [t]; 
				avPolarityIndexBIN [group] += polarityIndexBIN [t];  
				avXPolarityVectorSemiBIN [group] += xPolarityVectorSemiBIN [t]; 
				avYPolarityVectorSemiBIN [group] += yPolarityVectorSemiBIN [t];
				avZPolarityVectorSemiBIN [group] += zPolarityVectorSemiBIN [t];
				avPolarityVectorLengthSemiBIN [group] += polarityVectorLengthSemiBIN [t]; 
				avPolarityIndexSemiBIN [group] += polarityIndexSemiBIN [t]; 
			}
			for(int gr = 0; gr < projectedTimes; gr++){
				avXC [gr] /= (double) projectedFrames;
				avYC [gr] /= (double) projectedFrames;
				avZC [gr] /= (double) projectedFrames;
				avXCOM [gr] /= (double) projectedFrames; 
				avYCOM [gr] /= (double) projectedFrames;
				avZCOM [gr] /= (double) projectedFrames;
				avXSpan [gr] /= (double) projectedFrames;
				avYSpan [gr] /= (double) projectedFrames;
				avZSpan [gr] /= (double) projectedFrames;
				avAverageIntensity [gr] /= (double) projectedFrames;
				avMinimumIntensity [gr] /= (double) projectedFrames;
				avMaximumIntensity [gr] /= (double) projectedFrames;
				avSdIntensity [gr] /= (double) projectedFrames;
				avVolume [gr] /= (double) projectedFrames;
				avSurface [gr] /= (double) projectedFrames; 
				avRI [gr] /= (double) projectedFrames;
				avConvexHullVolume [gr] /= (double) projectedFrames;
				avConvexHullSurface [gr] /= (double) projectedFrames;
				avConvexHullxC [gr] /= (double) projectedFrames;
				avConvexHullyC [gr] /= (double) projectedFrames;
				avConvexHullzC [gr] /= (double) projectedFrames;
				avXPolarityVectorBIN [gr] /= (double) projectedFrames; 
				avYPolarityVectorBIN [gr] /= (double) projectedFrames;
				avZPolarityVectorBIN [gr] /= (double) projectedFrames;
				avPolarityVectorLengthBIN [gr] /= (double) projectedFrames;
				avPolarityIndexBIN [gr] /= (double) projectedFrames;
				avXPolarityVectorSemiBIN [gr] /= (double) projectedFrames;
				avYPolarityVectorSemiBIN [gr] /= (double) projectedFrames;
				avZPolarityVectorSemiBIN [gr] /= (double) projectedFrames;
				avPolarityVectorLengthSemiBIN [gr] /= (double) projectedFrames; 
				avPolarityIndexSemiBIN [gr] /= (double) projectedFrames;
			}
			
			//Skeleton parameters
			if(skeletonize){
				avFoundSkl = new double [projectedTimes];
				avBranches = new double [projectedTimes];
				avJunctions = new double [projectedTimes];
				avTips = new double [projectedTimes];
				avTriplePs = new double [projectedTimes];
				avQuadruplePs = new double [projectedTimes];
				avJunctionVx = new double [projectedTimes];
				avSlabVx = new double [projectedTimes];
				avBranchesOfAll = new double [projectedTimes];
				avJunctionsOfAll = new double [projectedTimes];
				avTipsOfAll = new double [projectedTimes];
				avTriplePsOfAll = new double [projectedTimes];
				avQuadruplePsOfAll = new double [projectedTimes];
				avJunctionVxOfAll = new double [projectedTimes];
				avSlabVxOfAll = new double [projectedTimes];
				avTreeLength = new double [projectedTimes];
				avAvBranchLength = new double [projectedTimes];
				avMaxBranchLength = new double [projectedTimes];
				avLargestShortestPath = new double [projectedTimes];
				avTreeLengthOfAll = new double [projectedTimes];
				avAvBranchLengthOfAll = new double [projectedTimes];
				avMaxBranchLengthOfAll = new double [projectedTimes];
				avLargestShortestPathOfAll = new double [projectedTimes];
				
				for(int t = 0; t < times-times%projectedFrames; t++){
					group = ((int)((double)t/projectedFrames));
					avFoundSkl [group] += 0.0 + foundSkl [t];
					avBranches [group] += 0.0 + branches [t];
					avJunctions [group] += 0.0 + junctions [t];
					avTips [group] += 0.0 + tips [t];
					avTriplePs [group] += 0.0 + triplePs [t];
					avQuadruplePs [group] += 0.0 + quadruplePs [t];
					avJunctionVx [group] += 0.0 + junctionVx [t];
					avSlabVx [group] += 0.0 + slabVx [t];
					avBranchesOfAll [group] += 0.0 + branchesOfAll [t];
					avJunctionsOfAll [group] += 0.0 + junctionsOfAll [t];
					avTipsOfAll [group] += 0.0 + tipsOfAll [t];
					avTriplePsOfAll [group] += 0.0 + triplePsOfAll [t];
					avQuadruplePsOfAll [group] += 0.0 + quadruplePsOfAll [t];
					avJunctionVxOfAll [group] += 0.0 + junctionVxOfAll [t];
					avSlabVxOfAll [group] += 0.0 + slabVxOfAll [t];
					avTreeLength [group] += 0.0 + treeLength [t];
					avAvBranchLength [group] += 0.0 + avBranchLength [t];
					avMaxBranchLength [group] += 0.0 + maxBranchLength [t];
					avLargestShortestPath [group] += 0.0 + largestShortestPath [t];
					avTreeLengthOfAll [group] += 0.0 + treeLengthOfAll [t];
					avAvBranchLengthOfAll [group] += 0.0 + avBranchLengthOfAll [t];
					avMaxBranchLengthOfAll [group] += 0.0 + maxBranchLengthOfAll [t];
					avLargestShortestPathOfAll [group] += 0.0 + largestShortestPathOfAll [t];
				}
				for(int gr = 0; gr < projectedTimes; gr++){
					avFoundSkl [gr] /= (double) projectedFrames;
					avBranches [gr] /= (double) projectedFrames;
					avJunctions [gr] /= (double) projectedFrames;
					avTips [gr] /= (double) projectedFrames;
					avTriplePs [gr] /= (double) projectedFrames;
					avQuadruplePs [gr] /= (double) projectedFrames;
					avJunctionVx [gr] /= (double) projectedFrames;
					avSlabVx [gr] /= (double) projectedFrames;
					avBranchesOfAll [gr] /= (double) projectedFrames;
					avJunctionsOfAll [gr] /= (double) projectedFrames;
					avTipsOfAll [gr] /= (double) projectedFrames;
					avTriplePsOfAll [gr] /= (double) projectedFrames;
					avQuadruplePsOfAll [gr] /= (double) projectedFrames;
					avJunctionVxOfAll [gr] /= (double) projectedFrames;
					avSlabVxOfAll [gr] /= (double) projectedFrames;
					avTreeLength [gr] /= (double) projectedFrames;
					avAvBranchLength [gr] /= (double) projectedFrames;
					avMaxBranchLength [gr] /= (double) projectedFrames;
					avLargestShortestPath [gr] /= (double) projectedFrames;
					avTreeLengthOfAll [gr] /= (double) projectedFrames;
					avAvBranchLengthOfAll [gr] /= (double) projectedFrames;
					avMaxBranchLengthOfAll [gr] /= (double) projectedFrames;
					avLargestShortestPathOfAll [gr] /= (double) projectedFrames;
				}
			}
			
//			progressDialog.notifyMessage("Step 4 LTPs 3 " + df.format(new Date()),ProgressDialog.NOTIFICATION);
			
			//Dynamic parameters
			if(times>1){
				avMovingVectorLengthBIN = new double [projectedTimes];
				avMovingVectorLengthSemiBIN = new double [projectedTimes];
				avOccupVolume = new double [projectedTimes];
				avLostVolume = new double [projectedTimes];
				avMotility = new double [projectedTimes];
				avDeltaVolume = new double [projectedTimes];
				avDeltaRI = new double [projectedTimes];
				
				avNrOfExtensions = new double [projectedTimes];
				avNrOfRetractions = new double [projectedTimes];
				avNrOfExtensionsMoreThan1Px = new double [projectedTimes];
				avNrOfRetractionsMoreThan1Px = new double [projectedTimes];
				
				for(int t = 0; t < times-times%projectedFrames; t++){
					group = ((int)((double)t/projectedFrames));
					avMovingVectorLengthBIN [group] += movingVectorLengthBIN [t];
					avMovingVectorLengthSemiBIN [group] += movingVectorLengthSemiBIN [t];
					avOccupVolume [group] += occupVolume [t];
					avLostVolume [group] += lostVolume [t];
					avMotility [group] += motility [t];
					avDeltaVolume [group] += deltaVolume [t];
					avDeltaRI [group] += deltaRI [t];	
					
					avNrOfExtensions [group] += nrOfExtensions [t];
					avNrOfRetractions [group] += nrOfRetractions [t];
					avNrOfExtensionsMoreThan1Px [group] += nrOfExtensionsMoreThan1Vx [t];
					avNrOfRetractionsMoreThan1Px [group] += nrOfRetractionsMoreThan1Vx [t];
					
					if((t+1)%projectedFrames==0){	
						t++;	
						/**Shift induces every averaged value group 
						to be generated from the same number of values 
						included into calculation as the first group*/
					}
				}
				
				for(int gr = 0; gr < projectedTimes; gr++){
					avMovingVectorLengthBIN [gr] /= ((double) projectedFrames - 1.0);
					avMovingVectorLengthSemiBIN [gr] /= ((double) projectedFrames - 1.0);
					avOccupVolume [gr] /= ((double) projectedFrames - 1.0);
					avLostVolume [gr] /= ((double) projectedFrames - 1.0);
					avMotility [gr] /= ((double) projectedFrames - 1.0);
					avDeltaVolume [gr] /= ((double) projectedFrames - 1.0);
					avDeltaRI [gr] /= ((double) projectedFrames - 1.0);
					avNrOfExtensions [gr] /= ((double) projectedFrames - 1.0);
					avNrOfRetractions [gr] /= ((double) projectedFrames - 1.0);
					avNrOfExtensionsMoreThan1Px [gr] /= ((double) projectedFrames - 1.0);
					avNrOfRetractionsMoreThan1Px [gr] /= ((double) projectedFrames - 1.0);					
				}
			}		
		}
		initialized = true;
//		progressDialog.notifyMessage("Done " + df.format(new Date()),ProgressDialog.NOTIFICATION);
	}
	
	/** Closes all ImagePlus belonging to the particle*/
	public void closeImps (){
		if(branches!=null){
			skeletonImp.changes = false;
			skeletonImp.close();
		}	
		particleImp.changes = false;
		particleImp.close();
		convexHullImp.changes = false;
		convexHullImp.close();
		System.gc();
	}
	
	/**
	 * @return a binary 3D image (as boolean array) containing the convex hull of the positive pixels
	 * in the binary image
	 * @param image
	 * @param progressDialog should reference to the current progressDialog that visualises analysis progress
	 * */
	private boolean [][][] getConv3DHull(boolean[][][] image, ProgressDialog progressDialog){	//TODO IMPROVE!
		int width = image.length;
		int height = image[0].length;
		int slices = image[0][0].length;
		
		progressDialog.addToBar(0.01);
		
		//initialize
			ArrayList <Point3D> pList = new ArrayList <Point3D>();
			ArrayList <Point3D> oldPList = new ArrayList <Point3D>();
			ArrayList <Point3D> oldPSaveList = new ArrayList <Point3D>();
			
			int itNewStCount [] = new int [slices];
			int itOldStCount [] = new int [slices];
			int iterativeNewSPCount = 0,
				iterativeOldSPCount = 0;
		//initialize
		
		//Create StackPointString
			
		//First 2D Hull -> point detection
			{
				Polygon p;
			boolean particlesAvailable;
				for(int z = 0; z < slices; z++){	
					itNewStCount [z] = 0;
					itOldStCount [z] = 0;
					
					p = new Polygon();
					particlesAvailable = false;
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							if(image[x][y][z]==true){
								p.addPoint(x,y);
								particlesAvailable = true;											
							}
						}
					}				
					
					if(particlesAvailable){
						p = new PolygonRoi(p,Roi.POLYGON).getConvexHull();	
						itNewStCount [z] = p.npoints;
						iterativeNewSPCount = p.npoints;
										
						pList.ensureCapacity(iterativeNewSPCount);
						oldPSaveList.ensureCapacity(iterativeNewSPCount);
						for(int i = 0; i < p.npoints; i++){
							pList.add(new Point3D(p.xpoints[i],p.ypoints[i],z));
							oldPSaveList.add(new Point3D(p.xpoints[i],p.ypoints[i],z));
					 	}
						
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){							
								if(p.contains(x,y)){
									image[x][y][z] = true;
								}else{
									image[x][y][z] = false;
								}
							}
						}						
					}	
				}				
			}
			System.gc();
		//First 2D Hull -> point detection
		
//		hulling: while(true){		
		//Calculate nr of connections to draw; To show process
			int connectionsCount = 0;
			
			int substractConnections = 0;
			for(int z = 0; z < slices; z++){
				substractConnections += (itNewStCount[z]);
				connectionsCount+=itNewStCount[z]*(iterativeNewSPCount+iterativeOldSPCount-substractConnections);
			}
			int doneConnectionsCount = 0;
		//Calculate nr of connections to draw; To show process
			
		//Get Convex Hull
			{
				ArrayList <Point3D> connectList1, connectList2;
				Point3D p;
				int dX, dY, dZ;
				int dPosArray [];
				int posMin;
				for(int slice = 0; slice < slices; slice++){
					if(itNewStCount[slice]>0){
						//generate lists to connect to	
						connectList1 = new ArrayList <Point3D> (itNewStCount[slice]);	//String connectStackPointString = "";
						connectList2 = new ArrayList <Point3D> (iterativeNewSPCount+iterativeOldSPCount);	
						System.gc();
						
							//String connectStackPointString = "";
						for(int l = 0; l < pList.size(); l++){
							p = new Point3D(pList.get(l).x,pList.get(l).y,pList.get(l).z);
							if(p.z==slice){
								connectList1.add(p);
							}else if(p.z>slice){
								connectList2.add(p);
							}					
						}
						connectList1.trimToSize();
						connectList2.trimToSize();
						
						if(iterativeOldSPCount!=0){
							for(int l = 0; l < oldPList.size(); l++){
								if(oldPList.get(l).z!=slice){
									p = new Point3D(oldPList.get(l).x,oldPList.get(l).y,oldPList.get(l).z);
									connectList2.add(p);
								}
							}	
						}										
						
						if(connectList2.size()>0){
							for(int pID = 0; pID < connectList1.size(); pID++){
								for(int cpID = 0; cpID < connectList2.size(); cpID++){								
									dX = connectList2.get(cpID).x - connectList1.get(pID).x;
									dY = connectList2.get(cpID).y - connectList1.get(pID).y;
									dZ = connectList2.get(cpID).z - connectList1.get(pID).z;
									
									dPosArray = new int [] {Math.abs(dX), Math.abs(dY), Math.abs(dZ)};
									java.util.Arrays.sort(dPosArray);
									
									posMin = dPosArray[0];								
									if(posMin == 0){
										posMin = dPosArray [1];
									}
									if(posMin == 0){
										posMin = dPosArray [2];
									}
									
									if(posMin > 1){
										if(posMin == Math.abs(dX)){ 
											double y = (double) connectList1.get(pID).y, z = (double) connectList1.get(pID).z;
											y+=(double)dY/(Math.abs(dX));
											z+=(double)dZ/(Math.abs(dX));
											if(dX>0){
												Walking: for(int x = connectList1.get(pID).x+(dX/Math.abs(dX)); x <= connectList2.get(cpID).x; x+=(dX/Math.abs(dX))){
													if((int)Math.round(y) < height && (int)Math.round(z) < slices && (int)Math.round(y) >= 0 && (int)Math.round(z) >= 0 && (int)Math.round(x) < width && (int)Math.round(x) >= 0){
														if(image[x][(int)Math.round(y)][(int)Math.round(z)]==true){
															break Walking;
														}
														image[x][(int)Math.round(y)][(int)Math.round(z)]=true;
													}								
													y+=(double)dY/(Math.abs(dX));
													z+=(double)dZ/(Math.abs(dX));
													if(dY!=0){
														if(dY>0){	if(y < connectList2.get(cpID).y){}else{break Walking;}	}else{	if(y > connectList2.get(cpID).y){}else{break Walking;}}
													}
													if(dZ!=0){
														if(dZ>0){	if(z < connectList2.get(cpID).z){}else{break Walking;}	}else{	if(z > connectList2.get(cpID).z){}else{break Walking;}}
													}
												}	
											}else{
												Walking: for(int x = connectList1.get(pID).x+(dX/Math.abs(dX)); x >= connectList2.get(cpID).x; x+=(dX/Math.abs(dX))){
													if((int)Math.round(y) < height && (int)Math.round(z) < slices && (int)Math.round(y) >= 0 && (int)Math.round(z) >= 0 && (int)Math.round(x) < width && (int)Math.round(x) >= 0){
														if(image[x][(int)Math.round(y)][(int)Math.round(z)]==true){
															break Walking;
														}
														image[x][(int)Math.round(y)][(int)Math.round(z)]=true;
													}
													y+=(double)dY/(Math.abs(dX));
													z+=(double)dZ/(Math.abs(dX));
													if(dY!=0){
														if(dY>0){	if(y <= connectList2.get(cpID).y){}else{break Walking;}	}else{	if(y >= connectList2.get(cpID).y){}else{break Walking;}}
													}
													if(dZ!=0){
														if(dZ>0){	if(z <= connectList2.get(cpID).z){}else{break Walking;}	}else{	if(z >= connectList2.get(cpID).z){}else{break Walking;}}
													}
												}
											}
											
										}else if(posMin == Math.abs(dY)){ 				
											double x = (double) connectList1.get(pID).x, z = (double) connectList1.get(pID).z;
											x+=(double)dX/(Math.abs(dY));
											z+=(double)dZ/(Math.abs(dY));
											if(dY>0){
												Walking: for(int y = connectList1.get(pID).y+(dY/Math.abs(dY)); y <= connectList2.get(cpID).y; y+=(dY/Math.abs(dY))){
													if((int)Math.round(y) < height && (int)Math.round(z) < slices && (int)Math.round(y) >= 0 && (int)Math.round(z) >= 0 && (int)Math.round(x) < width && (int)Math.round(x) >= 0){
														if(image[(int)Math.round(x)][y][(int)Math.round(z)]==true){
															break Walking;
														}
														image[(int)Math.round(x)][y][(int)Math.round(z)]=true;
													}								
													x+=(double)dX/(Math.abs(dY));
													z+=(double)dZ/(Math.abs(dY));
													if(dX!=0){
														if(dX>0){	if(x <= connectList2.get(cpID).x){}else{break Walking;}	}else{	if(x >= connectList2.get(cpID).x){}else{break Walking;}}
													}
													if(dZ!=0){
														if(dZ>0){	if(z <= connectList2.get(cpID).z){}else{break Walking;}	}else{	if(z >= connectList2.get(cpID).z){}else{break Walking;}}
													}
												}
											}else{
												Walking: for(int y = connectList1.get(pID).y+(dY/Math.abs(dY)); y >= connectList2.get(cpID).y; y+=(dY/Math.abs(dY))){
													if((int)Math.round(y) < height && (int)Math.round(z) < slices && (int)Math.round(y) >= 0 && (int)Math.round(z) >= 0 && (int)Math.round(x) < width && (int)Math.round(x) >= 0){
														if(image[(int)Math.round(x)][y][(int)Math.round(z)]==true){
															break Walking;
														}
														image[(int)Math.round(x)][y][(int)Math.round(z)]=true;
													}
													x+=(double)dX/(Math.abs(dY));
													z+=(double)dZ/(Math.abs(dY));
													if(dX!=0){
														if(dX>0){	if(x <= connectList2.get(cpID).x){}else{break Walking;}	}else{	if(x >= connectList2.get(cpID).x){}else{break Walking;}}
													}
													if(dZ!=0){
														if(dZ>0){	if(z <= connectList2.get(cpID).z){}else{break Walking;}	}else{	if(z >= connectList2.get(cpID).z){}else{break Walking;}}
													}
												}
											}
												
										}else if(posMin == Math.abs(dZ)){
											double y = (double) connectList1.get(pID).y, x = (double) connectList1.get(pID).x;
											x+=(double)dX/(Math.abs(dZ));
											y+=(double)dY/(Math.abs(dZ));
											if(dZ>0){
												Walking: for(int z = connectList1.get(pID).z+(dZ/Math.abs(dZ)); z <= connectList2.get(cpID).z; z+=(dZ/Math.abs(dZ))){
													if((int)Math.round(y) < height && (int)Math.round(z) < slices && (int)Math.round(y) >= 0 && (int)Math.round(z) >= 0 && (int)Math.round(x) < width && (int)Math.round(x) >= 0){
														if(image[(int)Math.round(x)][(int)Math.round(y)][z]==true){
															break Walking;
														}
														image[(int)Math.round(x)][(int)Math.round(y)][z]=true;
													}
													x+=(double)dX/(Math.abs(dZ));
													y+=(double)dY/(Math.abs(dZ));
													if(dY!=0){
														if(dY>0){	if(y <= connectList2.get(cpID).y){}else{break Walking;}	}else{	if(y >= connectList2.get(cpID).y){}else{break Walking;}}
													}
													if(dX!=0){
														if(dX>0){	if(x <= connectList2.get(cpID).x){}else{break Walking;}	}else{	if(x >= connectList2.get(cpID).x){}else{break Walking;}}
													}
												}	
											}else{
												Walking: for(int z = connectList1.get(pID).z+(dZ/Math.abs(dZ)); z >= connectList2.get(cpID).z; z+=(dZ/Math.abs(dZ))){
													if((int)Math.round(y) < height && (int)Math.round(z) < slices && (int)Math.round(y) >= 0 && (int)Math.round(z) >= 0 && (int)Math.round(x) < width && (int)Math.round(x) >= 0){
														if(image[(int)Math.round(x)][(int)Math.round(y)][z]==true){
															break Walking;
														}
														image[(int)Math.round(x)][(int)Math.round(y)][z]=true;
													}
													x+=(double)dX/(Math.abs(dZ));
													y+=(double)dY/(Math.abs(dZ));
													if(dY!=0){
														if(dY>0){	if(y <= connectList2.get(cpID).y){}else{break Walking;}	}else{	if(y >= connectList2.get(cpID).y){}else{break Walking;}}
													}
													if(dX!=0){
														if(dX>0){	if(x <= connectList2.get(cpID).x){}else{break Walking;}	}else{	if(x >= connectList2.get(cpID).x){}else{break Walking;}}
													}
												}	
											}
											
										}	
									}
									doneConnectionsCount++;
								}
														
								if((int)Math.round(100.0*doneConnectionsCount/(double)connectionsCount) % 10 == 0){
									progressDialog.updateBarText("Get convex hull ... " + (int)Math.round(100.0*doneConnectionsCount/(double)connectionsCount) + "%");
								}	
							}
						}
							
					}
				}
			}			
			progressDialog.updateBarText("Get convex hull ... 100%");
									
			//get 2D hull
			{
				iterativeNewSPCount = 0;
				iterativeOldSPCount = 0;
				Polygon p;
				LinkedList<Point3D> pList2 = new LinkedList<Point3D>(); 
				LinkedList<Point3D> oldPList2 = new LinkedList<Point3D>(); 
				for(int z = 0; z < slices; z++){
					itNewStCount [z] = 0;
					itOldStCount [z] = 0;
					
					p = new Polygon();
					boolean particlesAvailable = false;
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							if(image[x][y][z]==true){
								p.addPoint(x,y);
								particlesAvailable = true;											
							}
						}
					}				
					
					if(particlesAvailable){		
						p = new PolygonRoi(p,Roi.POLYGON).getConvexHull();					
						
						for(int i = 0; i < p.npoints; i++){		
							boolean found = false;
							searchForEqual: for(int j = 0; j < oldPSaveList.size(); j++){	
								if(oldPSaveList.get(j).x == p.xpoints[i]
										&& oldPSaveList.get(j).y == p.ypoints[i]
										&& oldPSaveList.get(j).z == z){
									found = true;
									break searchForEqual;
								}
							}	
							if(found == true){
								iterativeOldSPCount++;
								oldPList2.add(new Point3D(p.xpoints[i],p.ypoints[i],z));
								itOldStCount[z]++;
							}else{
								iterativeNewSPCount++;
								pList2.add(new Point3D(p.xpoints[i],p.ypoints[i],z));
								itNewStCount[z]++;
							}
					 	}						
						for(int x = 0; x < width; x++){
							for(int y = 0; y < height; y++){							
								if(p.contains(x,y) && image[x][y][z] == false){
									image[x][y][z] = true;
								}
							}
						}						
					}	
				}
				
				//replace old point list
				pList.clear();
				oldPList.clear();
				oldPSaveList.clear();
				
				pList.ensureCapacity(iterativeNewSPCount);
				oldPList.ensureCapacity(iterativeOldSPCount);
				oldPSaveList.ensureCapacity(iterativeOldSPCount+iterativeNewSPCount);
				
				for(int j = 0; j < pList2.size(); j++){	
					pList.add(new Point3D(pList2.get(j).x,pList2.get(j).y,pList2.get(j).z));
					oldPSaveList.add(new Point3D(pList2.get(j).x,pList2.get(j).y,pList2.get(j).z));
				}				
				
				//replace old point list				
				for(int j = 0; j < oldPList2.size(); j++){	
					oldPList.add(new Point3D(oldPList2.get(j).x,oldPList2.get(j).y,oldPList2.get(j).z));
					oldPSaveList.add(new Point3D(oldPList2.get(j).x,oldPList2.get(j).y,oldPList2.get(j).z));
				}
				pList2.clear();
				oldPList2.clear();				
			}
			progressDialog.updateBarText("convex hull determined");
			oldPList.clear();
			pList.clear();
			return image;
//		}	
	}
	
	/**
	 * @return a modified copy of the @param skeleton ImagePlus, 
	 * in which all skeleton parts are deleted which do not overlap 
	 * with positive voxels in the ImagePlus @param original ImagePlus
	 * */
	@SuppressWarnings("unused")
	private ImagePlus pruneSkeleton(ImagePlus original, ImagePlus skeleton){
		ImagePlus skl = skeleton.duplicate();
		for(int t = 0; t < original.getNFrames(); t++){
			for(int z = 0; z < original.getNSlices(); z++){
				for(int x = 0; x < original.getWidth(); x++){
					for(int y = 0; y < original.getHeight(); y++){
						if(skl.getStack().getVoxel(x, y, skl.getStackIndex(1, t+1, t+1) - 1)>0.0
								&& original.getStack().getVoxel(x, y, original.getStackIndex(1, t+1, t+1) - 1)==0.0){
							skl.getStack().setVoxel(x, y, skl.getStackIndex(1, t+1, t+1) - 1,0.0);
						}
					}
				}
			}
		}
		return skl;
	}
	
	/**
	 * Saves a 3D visualization of the ImagePlus imp using a modified code of the ImageJ plugin "Volume Viewer" by Kai Uwe Barthel (2012), 
	 * which is implemented in FIJI (https://github.com/fiji/Volume_Viewer) and was released under the Public Domain license.
	 * @param savePath = path, where to save image of 3D visualization as .tif
	 * @param RPSuffix = String added to the path of the cell volume image to indicate which size-filter was used in detection
	 * @param v3D = the visualizer containing the settings to create the Visualisations
	 * */
	public void save3DVisualizations(String savePath, String RPSuffix, Visualizer3D v3D){
		if(particleImp.getNSlices()==1)	return;
		//add bar
		int pxNrX, pxNrY, pxNrZ;
		float calBarLength = 100000.0f;
		boolean two = false;
		selecting: while(true){
			pxNrX = (int)Math.round(calBarLength / particleImp.getCalibration().pixelWidth);
			pxNrY = (int)Math.round(calBarLength / particleImp.getCalibration().pixelHeight);
			pxNrZ = (int)Math.round(calBarLength / particleImp.getCalibration().pixelDepth);
			if(pxNrX > particleImp.getWidth()
					|| pxNrY > particleImp.getHeight()
					|| pxNrZ > particleImp.getNSlices()){			
				if(two){
					calBarLength /= 2.0f;
					two = false;
				}else{
					calBarLength /= 5.0f;
					two = true;
				}		
			}else{
				break selecting;
			}
		}
		
		ImagePlus impCal, imp3D, impOut;
		ImageStack stackOut;
		
		v3D.setImage(getTimePointFor3D(particleImp, 1));
		int width =  v3D.getWidth(),
			height = v3D.getHeight();
		
		TextPanel tp;
		tp = new TextPanel("Info");
		tp.append("bars = " + calBarLength + " " + particleImp.getCalibration().getUnit());
		tp.append("3D visualizations were generated via the ImageJ plugin <Volume Viewer 2.0> (27.11.2012, (C) Kai Uwe Barthel)");
		tp.saveAs(savePath + "_3Dinfo.txt");
		
		//save 3D visualization of the particle imp
		{
			stackOut = new ImageStack(width,height);
			v3D.setObjectLightValue(1.2f);
			v3D.setLightPosX(-0.25f);
			v3D.setAlphaOffset1(0);
			
			for(int i = 0; i < particleImp.getNFrames(); i++){
				impCal = getTimePointFor3D(particleImp, i+1);
				impCal = this.getContained3DScBar(impCal, pxNrX, pxNrY, pxNrZ);
				convertToRGB(impCal);
				
//				if(impCal == null) impCal = particleImp.duplicate();
				
				v3D.setImage(impCal);
				imp3D = v3D.get3DVisualization();
				
				impCal.changes = false;
				impCal.close();
				
				stackOut.addSlice(imp3D.getProcessor());
				
				imp3D.changes = false;
				imp3D.close();
			}			
			
			impOut = IJ.createImage("3D", width, height, particleImp.getNFrames(), 24);
			impOut.setStack(stackOut);
			IJ.saveAsTiff(impOut, savePath + RPSuffix + "-3D.tif");

			impOut.changes = false;
			impOut.close();
		}
		
		//save 3D visualization of the convex hull imp
		{
			stackOut = new ImageStack(width,height);
			v3D.setObjectLightValue(1.2f);
			v3D.setLightPosX(0.0f);
			v3D.setAlphaOffset1(-45);
			for(int i = 0; i < convexHullImp.getNFrames(); i++){
				impCal = getTimePointFor3DHull(i+1);
				impCal = this.getContained3DScBar(impCal, pxNrX, pxNrY, pxNrZ);
//				ImageConverter iCv = new ImageConverter(impCal);
//				iCv.convertToRGB();
				convertToRGB(impCal);
				
				v3D.setImage(impCal);
				imp3D = v3D.get3DVisualization();
				
				impCal.changes = false;
				impCal.close();
				
				stackOut.addSlice(imp3D.getProcessor());
				
				imp3D.changes = false;
				imp3D.close();
			}
			
			impOut = IJ.createImage("3D", width, height, convexHullImp.getNFrames(), 24);
			impOut.setStack(stackOut);
			
			IJ.saveAsTiff(impOut, savePath + "H-3D.tif");
			
			impOut.changes = false;
			impOut.close();			
		}
		
		//save 3D visualization of the particle imp
		if(skeletonImp!=null){
			stackOut = new ImageStack(width,height);
			v3D.setObjectLightValue(2.0f);
			v3D.setLightPosX(-0.25f);
			v3D.setAlphaOffset1(0);
			for(int i = 0; i < skeletonImp.getNFrames(); i++){
				impCal = getTimePointFor3DSkl(i+1);
				impCal = this.getContained3DScBar(impCal, pxNrX, pxNrY, pxNrZ);
				convertToRGB(impCal);
			
				v3D.setImage(impCal);
				imp3D = v3D.get3DVisualization();
				
				impCal.changes = false;
				impCal.close();
				
				stackOut.addSlice(imp3D.getProcessor());
				
				imp3D.changes = false;
				imp3D.close();
			}
			
			impOut = IJ.createImage("3D", width, height, skeletonImp.getNFrames(), 24);
			impOut.setStack(stackOut);
			
			IJ.saveAsTiff(impOut, savePath + "Skl-3D.tif");

			impOut.changes = false;
			impOut.close();
		}
		System.gc();
	}
	
	/**
	 * automatic conversion to RGB
	 * */
	private void convertToRGB(ImagePlus imp){
		if(imp.isComposite()){
			Calibration cal = imp.getCalibration();
			RGBStackConverter.convertToRGB(imp);
			imp.setCalibration(cal);
		}else{
//
//			ImageConverter iCv = new ImageConverter(imp);
//			iCv.convertToRGB();
//			
			int nSlices = imp.getStackSize();
			ImageStack stack1 = imp.getStack(),
					stack2 = new ImageStack(imp.getWidth(), imp.getHeight());
	        String label;
	        ImageProcessor ip1, ip2;
	        Calibration cal = imp.getCalibration();
	        for(int i = 1; i <= nSlices; i++) {
	            label = stack1.getSliceLabel(i);
	            ip1 = stack1.getProcessor(i);
	            ip2 = ip1.convertToRGB();
	            stack2.addSlice(label, ip2);
	        }
	        imp.setStack(stack2);
	        imp.setCalibration(cal);
		}	        
	}
	
	
	/**
	 * @param imp = ImagePlus where to derive time-point from
	 * @param t = frame index, one-based (1 <= t <= imp.getNFrames())
	 * */
	private static ImagePlus getTimePointFor3D(ImagePlus imp, int t){
		ImagePlus impOut = IJ.createHyperStack(imp.getTitle() + " t" + t, 
				imp.getWidth(), imp.getHeight(), 3, imp.getNSlices()+2, 1, 8);
		int iMax = MotiQ_3D.getMaxIntensity(impOut);
		impOut.setCalibration(imp.getCalibration());
		for(int c = 0; c < 3; c++){
			for(int s = 0; s < imp.getNSlices(); s++){
				for(int x = 0; x < imp.getWidth(); x++){
					for(int y = 0; y < imp.getHeight(); y++){
						if(imp.getStack().getVoxel(x, y, imp.getStackIndex(1, s+1, t)-1)>0.0){
							impOut.getStack().setVoxel(x, y, impOut.getStackIndex(c+1,s+2,1)-1, iMax);
						}						
					}
				}
			}
		}						
		return impOut;
	}
	
	/**
	 * @param t = frame index, one-based (1 <= t <= imp.getNFrames())
	 * */
	private ImagePlus getTimePointFor3DHull(int t){
		ImagePlus impOut = IJ.createHyperStack(particleImp.getTitle() + " t" + t, 
				particleImp.getWidth(), particleImp.getHeight(), 3, particleImp.getNSlices()+2, 1, 8);
		impOut.setCalibration(particleImp.getCalibration());
		int iMax = MotiQ_3D.getMaxIntensity(impOut);
		{
			for(int s = 0; s < particleImp.getNSlices(); s++){
				for(int x = 0; x < particleImp.getWidth(); x++){
					for(int y = 0; y < particleImp.getHeight(); y++){
						if(convexHullImp.getStack().getVoxel(x, y, convexHullImp.getStackIndex(1, s+1, t)-1)>0.0){
							if(particleImp.getStack().getVoxel(x, y, particleImp.getStackIndex(1, s+1, t)-1)>0.0){
								impOut.getStack().setVoxel(x, y, impOut.getStackIndex(2,s+2,1)-1, iMax/4.0);
							}
							impOut.getStack().setVoxel(x, y, impOut.getStackIndex(3,s+2,1)-1, iMax/3.0);							
						}else if(particleImp.getStack().getVoxel(x, y, particleImp.getStackIndex(1, s+1, t)-1)>0.0){
							impOut.getStack().setVoxel(x, y, impOut.getStackIndex(2,s+2,1)-1, iMax);
						}
									
					}
				}
			}
		}			
		
		impOut.setC(1);
		IJ.run(impOut, "Red", "");
		impOut.setC(2);
		IJ.run(impOut, "Green", "");	
		impOut.setC(3);
		IJ.run(impOut, "Blue", "");
		
		impOut.setDisplayMode(IJ.COMPOSITE);
		impOut.setActiveChannels("111");
		
		return impOut;
	}
	
	/**
	 * @param t = frame index, one-based (1 <= t <= imp.getNFrames())
	 * */
	private ImagePlus getTimePointFor3DSkl(int t){
		ImagePlus impOut = IJ.createHyperStack(skeletonImp.getTitle() + " t" + t, 
				skeletonImp.getWidth(), skeletonImp.getHeight(), 3, skeletonImp.getNSlices()+2, 1, 
				8);
		impOut.setCalibration(skeletonImp.getCalibration());
		int iMax = MotiQ_3D.getMaxIntensity(impOut);
		{
			for(int s = 0; s < skeletonImp.getNSlices(); s++){
				for(int x = 0; x < skeletonImp.getWidth(); x++){
					for(int y = 0; y < skeletonImp.getHeight(); y++){
						if(skeletonImp.getStack().getVoxel(x, y, skeletonImp.getStackIndex(1, s+1, t)-1)>0.0){
							if(skeletonImp.getStack().getVoxel(x, y, skeletonImp.getStackIndex(1, s+1, t)-1)<60.0){
								//tip = blue
								impOut.getStack().setVoxel(x,y,impOut.getStackIndex(3,s+2,1)-1,
										iMax);
							}else if(skeletonImp.getStack().getVoxel(x, y, skeletonImp.getStackIndex(1, s+1, t)-1)<100.0){
								//crossing = purple
								impOut.getStack().setVoxel(x,y,impOut.getStackIndex(1,s+2,1)-1,
										iMax);
								impOut.getStack().setVoxel(x,y,impOut.getStackIndex(3,s+2,1)-1,
										iMax);
							}else{
								//branch = orange
								impOut.getStack().setVoxel(x,y,impOut.getStackIndex(1,s+2,1)-1,
										iMax);
								impOut.getStack().setVoxel(x,y,impOut.getStackIndex(2,s+2,1)-1,
										iMax/2.0);								
							}
						}			
					}
				}
			}
		}			
		
		impOut.setDisplayMode(IJ.COMPOSITE);
		impOut.setActiveChannels("111");
		
		return impOut;
	}
	
	private ImagePlus getContained3DScBar(ImagePlus imp2, int pxNrX, int pxNrY, int pxNrZ){
		int orX = 0, orY = imp2.getHeight()-1, orZ = imp2.getNSlices()-1;
		ImagePlus imp = imp2.duplicate();
		double maxValue = MotiQ_3D.getMaxIntensity(imp);
		
		double thicknessCal = imp.getCalibration().pixelHeight;
		if(thicknessCal < imp.getCalibration().pixelWidth) thicknessCal = imp.getCalibration().pixelWidth;
		if(thicknessCal < imp.getCalibration().pixelDepth) thicknessCal = imp.getCalibration().pixelDepth;
		
		int thicknessX = (int)Math.round(thicknessCal / imp.getCalibration().pixelHeight), 
			thicknessY = (int)Math.round(thicknessCal / imp.getCalibration().pixelWidth), 
			thicknessZ = (int)Math.round(thicknessCal / imp.getCalibration().pixelDepth);
		if(thicknessX < 1)	thicknessX = 1;
		if(thicknessY < 1)	thicknessY = 1;
		if(thicknessZ < 1)	thicknessZ = 1;
		
		try{
			for(int i = 0; i < pxNrX; i++){
				for(int j = 0; j < thicknessY; j++){
					for(int k = 0; k < thicknessZ; k++){
						for(int c = 0; c < imp.getNChannels(); c++){
							imp.getStack().setVoxel(orX + i, orY - j,imp.getStackIndex(c+1, orZ-k+1, 1)-1, maxValue);
						}						
					}
				}		
			}
			for(int i = 0; i < pxNrY; i++){
				for(int j = 0; j < thicknessX; j++){
					for(int k = 0; k < thicknessZ; k++){
						for(int c = 0; c < imp.getNChannels(); c++){
							imp.getStack().setVoxel(orX + j, orY - i, imp.getStackIndex(c+1, orZ-k+1, 1)-1, maxValue);
						}						
					}
				}
			}
			for(int i = 0; i < pxNrZ; i++){
				for(int j = 0; j < thicknessX; j++){
					for(int k = 0; k < thicknessY; k++){
						for(int c = 0; c < imp.getNChannels(); c++){
							imp.getStack().setVoxel(orX + j, orY - k, imp.getStackIndex(c+1, orZ-i+1, 1)-1, maxValue);
						}						
					}
				}
			}
			return imp;
		}catch(Exception e){
			return null;
		}		
	}
	
//	private void addTimeStamp(ImagePlus imp){
		//TODO
//	}
}