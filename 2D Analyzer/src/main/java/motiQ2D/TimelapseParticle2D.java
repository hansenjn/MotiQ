/***===============================================================================
 *  
 * MotiQ_2D plugin for imageJ
 * 
 * Copyright (C) 2014-2023 Jan N. Hansen
 * First version: November 07, 2014   
 * This Version: April 15, 2019
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

package motiQ2D;

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
import motiQ2D.skeleton_analysis.*;
import motiQ2D.skeletonize3D.*;

class TimelapseParticle2D{
	public boolean initialized = false;
	double pointList [][];	//[nr][x,y,time]
	double data [][][];
	boolean hullData [][][];
	int tMin, tMax, times, xMin, xMax, width, yMin, yMax, height, projectedTimes;		
	int time [];
	double pxArea;
	
	//Morphological static parameters
	public ImagePlus convexHullImp, particleImp;
	double xC [], yC [], 
	xCOM [], yCOM [],
	xSpan [], ySpan [],
	averageIntensity [],
	minimumIntensity [],	
	maximumIntensity [],
	sdIntensity [],
	area [], 
	outline [],
	RI [],
	convexHullArea [],
	convexHullOutline [],
	convexHullxC [],
	convexHullyC [],
	xPolarityVectorBIN [],
	yPolarityVectorBIN [],
	polarityVectorLengthBIN [],
	polarityIndexBIN [], 
	xPolarityVectorSemiBIN [],
	yPolarityVectorSemiBIN [],
	polarityVectorLengthSemiBIN [],
	polarityIndexSemiBIN [];
	
	//Skeleton parameters
	public ImagePlus skeletonImp;
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
	shortestPath [],
	treeLengthOfAll [],
	avBranchLengthOfAll [],
	maxBranchLengthOfAll [],
	shortestPathOfAll [];
	
	//Dynamic parameters
	double movingVectorLengthBIN [],
	movingVectorLengthSemiBIN [],
	occupArea [],
	lostArea [],
	motility [],
	deltaArea [],
	deltaRI [];
	int nrOfExtensions [],
	nrOfRetractions [],
	nrOfExtensionsMoreThan1Px [],
	nrOfRetractionsMoreThan1Px [];
			
	//Long term dynamic parameters
	double projectedArea [],
	projectedStaticArea [],
	projectedDynamicFraction [],
	projectedConvexHullArea [],
	projectedConvexHullStaticArea [],
	projectedConvexHullDynamicFraction [];	
	double accumulatedDistanceBIN [],
	euclideanDistanceBIN [],
	directionalityIndexBIN [],
	accumulatedDistanceSemiBIN [],
	euclideanDistanceSemiBIN [],
	directionalityIndexSemiBIN [];
	
	//Long term averages of parameters
	double avXC [], avYC [], 
	avXCOM [], avYCOM [],
	avXSpan [], avYSpan [],
	avAverageIntensity [],
	avMinimumIntensity [],
	avMaximumIntensity [],
	avSdIntensity [],
	avArea [], 
	avOutline [],
	avRI [],
	avConvexHullArea [],
	avConvexHullOutline [],
	avConvexHullxC [],
	avConvexHullyC [],
	avXPolarityVectorBIN [],
	avYPolarityVectorBIN [],
	avPolarityVectorLengthBIN [],
	avPolarityIndexBIN [], 
	avXPolarityVectorSemiBIN [],
	avYPolarityVectorSemiBIN [],
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
	avShortestPath [],
	avTreeLengthOfAll [],
	avAvBranchLengthOfAll [],
	avMaxBranchLengthOfAll [],
	avShortestPathOfAll [];
	
	double avMovingVectorLengthBIN [],
	avMovingVectorLengthSemiBIN [],
	avOccupArea [],
	avLostArea [],
	avMotility [],
	avDeltaArea [],
	avDeltaRI [],
	avNrOfExtensions [],
	avNrOfRetractions [],
	avNrOfRetractionsMoreThan1Px [],
	avNrOfExtensionsMoreThan1Px [];
	
	//noninitilaized timelapseParticle
	public TimelapseParticle2D(){}	
	public TimelapseParticle2D(ArrayList<point> points, Calibration cal, double projectedFrames, boolean skeletonize, double gaussSigma, int orWidth, int orHeight, int orTimes, boolean minimizeImages, boolean binarizeBeforeSkeletonization){		
		double calibration = cal.pixelWidth;
		pxArea = calibration*calibration;
		double timePerFrame = cal.frameInterval;
		
		int nPoints = points.size();
		pointList = new double [nPoints][4];	//x,y,t,intensity
		
		//get min/max values
		tMin = Integer.MAX_VALUE;
		tMax = 0;
		xMin = Integer.MAX_VALUE;
		xMax = 0;
		yMin = Integer.MAX_VALUE;
		yMax = 0;
		for(int i = 0; i < nPoints; i++){
			pointList[i][0] = points.get(i).x*calibration;
			pointList[i][1] = points.get(i).y*calibration;
			pointList[i][2] = points.get(i).t*timePerFrame;	
			pointList[i][3] = points.get(i).intensity;	
			if(pointList[i][2]/timePerFrame>tMax)	tMax = (int)Math.round(pointList[i][2]/timePerFrame);
			if(pointList[i][2]/timePerFrame<tMin)	tMin = (int)Math.round(pointList[i][2]/timePerFrame);
			if(pointList[i][0]/calibration>xMax)		xMax = (int)Math.round(pointList[i][0]/calibration);
			if(pointList[i][0]/calibration<xMin)		xMin = (int)Math.round(pointList[i][0]/calibration);
			if(pointList[i][1]/calibration>yMax)		yMax = (int)Math.round(pointList[i][1]/calibration);
			if(pointList[i][1]/calibration<yMin)		yMin = (int)Math.round(pointList[i][1]/calibration);
		}
		points.clear();
		System.gc();
		
		times = 1+tMax-tMin;
		width = 1+xMax-xMin;
		height = 1+yMax-yMin;
		data = new double [width][height][times];
		
		//Determine static variables
		double maxIntensity = 0.0, maxPossibIntensity = 0.0;
		{
			xC = new double [times];
			yC = new double [times];
			xCOM = new double [times];	double xCOMsum [] = new double [times];
			yCOM = new double [times];	double yCOMsum [] = new double [times];
			xSpan = new double [times];	double minX [] = new double [times];	double maxX [] = new double [times];
			ySpan = new double [times];	double minY [] = new double [times];	double maxY [] = new double [times];
			averageIntensity = new double [times];	
			minimumIntensity = new double [times];	
			maximumIntensity = new double [times];	
			sdIntensity = new double [times];	
			area = new double [times];
			outline = new double [times];
			RI = new double [times];
			
			LinkedList<Polygon> convexHullPolygon = new LinkedList<Polygon>();
			convexHullArea = new double [times];
			convexHullOutline = new double [times];
			convexHullxC = new double [times];
			convexHullyC = new double [times];
			xPolarityVectorBIN = new double [times];
			yPolarityVectorBIN = new double [times];
			polarityVectorLengthBIN = new double [times];
			polarityIndexBIN = new double [times];
			xPolarityVectorSemiBIN = new double [times];
			yPolarityVectorSemiBIN = new double [times];
			polarityVectorLengthSemiBIN = new double [times];
			polarityIndexSemiBIN = new double [times];
			
			for(int t = 0; t < times; t++){
				xC [t] = 0.0;	yC [t] = 0.0;
				xCOM [t] = 0.0;	yCOM [t] = 0.0;
				xCOMsum [t] = 0.0;	yCOMsum [t] = 0.0;
				minX [t] = Double.POSITIVE_INFINITY;
				maxX [t] = 0.0;
				minY [t] = Double.POSITIVE_INFINITY;
				maxY [t] = 0.0;
				averageIntensity [t] = 0.0;	
				minimumIntensity [t] = Double.POSITIVE_INFINITY;	
				maximumIntensity [t] = 0.0;	
				sdIntensity [t] = 0.0;	
				area [t] = 0.0;
				outline [t] = 0.0;
				
				convexHullPolygon.add(new Polygon());
				convexHullArea [t] = 0.0;
				convexHullOutline [t] = 0.0;
				convexHullxC [t] = 0.0;
				convexHullyC [t] = 0.0;
			
				for(int x = 0; x < width; x++){
					for(int y = 0; y < height; y++){
						data [x][y][t] = 0.0;	
					}
				}
			}
			
			int tcal, xcal, ycal;
			for(int i = 0; i < nPoints; i++){
				if(pointList[i][3]>maxIntensity){
					maxIntensity=pointList[i][3];
				}
				
				tcal = (int)Math.round(pointList [i][2] / timePerFrame)-tMin;				
				xC [tcal] += pointList[i][0];
				yC [tcal] += pointList[i][1];
				xCOM [tcal] += pointList[i][0]*pointList[i][3];
				yCOM [tcal] += pointList[i][1]*pointList[i][3];
				xCOMsum [tcal] += pointList[i][3];
				yCOMsum [tcal] += pointList[i][3];
				
				if(pointList[i][0] > maxX [tcal]){	maxX [tcal] = pointList[i][0];}
				if(pointList[i][0] < minX [tcal]){	minX [tcal] = pointList[i][0];}
				if(pointList[i][1] > maxY [tcal]){	maxY [tcal] = pointList[i][1];}
				if(pointList[i][1] < minY [tcal]){	minY [tcal] = pointList[i][1];}
				
				averageIntensity [tcal] += pointList [i][3];	
				if(pointList[i][3] > maximumIntensity [tcal]){
					maximumIntensity [tcal] = pointList[i][3];
				}
				if(pointList[i][3] < minimumIntensity [tcal]){
					minimumIntensity [tcal] = pointList[i][3];
				}					
				
				area [tcal] += pxArea;
				
				data [(int)Math.round(pointList [i][0] / calibration)-xMin][(int)Math.round(pointList [i][1] / calibration)-yMin][tcal] = pointList[i][3];
				convexHullPolygon.get(tcal).addPoint((int)Math.round(pointList [i][0] / calibration)-xMin, (int)Math.round(pointList [i][1] / calibration)-yMin);
			}
			
			for(int t = 0; t < times; t++){		
				averageIntensity [t] /= (area [t] / pxArea);
			}
			
			for(int i = 0; i < nPoints; i++){									
				xcal = (int)Math.round(pointList [i][0] / calibration);
				ycal = (int)Math.round(pointList [i][1] / calibration);
				tcal = (int)Math.round(pointList [i][2] / timePerFrame)-tMin;
				
				sdIntensity [tcal] += Math.pow(pointList [i][3] - averageIntensity [tcal], 2.0);	
				
				//Look for other cell particles in surroundings
				double transientOutline = 0.0;
				if(ycal-1>=yMin){ 
					if(data[xcal-xMin][ycal-1-yMin][tcal]==0.0){transientOutline+=calibration;}
				}else{transientOutline+=calibration;}
				if(ycal+1<=yMax){ 
					if(data[xcal-xMin][ycal+1-yMin][tcal]==0.0){transientOutline+=calibration;}
				}else{transientOutline+=calibration;}
				if(xcal-1>=xMin){ 
					if(data[xcal-1-xMin][ycal-yMin][tcal]==0.0){transientOutline+=calibration;}
				}else{transientOutline+=calibration;}
				if(xcal+1<=xMax){ 
					if(data[xcal+1-xMin][ycal-yMin][tcal]==0.0){transientOutline+=calibration;}
				}else{transientOutline+=calibration;}
				//Look for other cell particles in surroundings
				outline [tcal] += transientOutline;
			}
			
			for(int t = 0; t < times; t++){
				xC [t] /= (area [t]/pxArea);
				yC [t] /= (area [t]/pxArea);
				xCOM [t] /= xCOMsum [t];
				yCOM [t] /= yCOMsum [t];
				xC [t] += calibration / 2.0;
				yC [t] += calibration / 2.0;
				xCOM [t] += calibration / 2.0;
				yCOM [t] += calibration / 2.0;
				xSpan [t] = maxX [t] - minX [t] + calibration;
				ySpan [t] = maxY [t] - minY [t] + calibration;
				sdIntensity [t] /= (area [t] / pxArea)-1.0;
				sdIntensity [t] = Math.sqrt(sdIntensity[t]);
				
				RI [t] = (outline[t])/(2*Math.sqrt(area[t]*Math.PI));	//RI
				convexHullPolygon.set(t,new PolygonRoi(convexHullPolygon.get(t),Roi.POLYGON).getConvexHull());
			}
			{
				String bitDepth = "8-bit"; maxPossibIntensity = 255.0;
				if(maxIntensity>255.0){bitDepth = "16-bit"; maxPossibIntensity = 65535.0;}
				if(maxIntensity>65535.0){bitDepth = "32-bit"; maxPossibIntensity = 2147483647.0;}
				if(minimizeImages){
					particleImp = IJ.createImage("Particle image", bitDepth, width+4, height+4, 1, times, 1);
				}else{
					particleImp = IJ.createImage("Particle image", bitDepth, orWidth, orHeight, 1, orTimes, 1);
				}				
				particleImp.setCalibration(cal);
			}
			
			hullData = new boolean [width][height][times];
			if(minimizeImages){
				for(int t = 0; t < times; t++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							//ConvexHullData
							if(convexHullPolygon.get(t).contains(x,y)){
								hullData [x][y][t] = true;
								convexHullArea [t] += pxArea;
								convexHullxC [t] += (double)(x+xMin);
								convexHullyC [t] += (double)(y+yMin);						
							}else{
								hullData [x][y][t] = false;
							}	
							//Generate Particle Image
							if(data[x][y][t] != 0.0){
								particleImp.getStack().setVoxel(x+2,y+2,t,data[x][y][t]);
							}else{
								particleImp.getStack().setVoxel(x+2,y+2,t,0.0);
							}
						}
					}
				}
			}else{
				for(int t = 0; t < times; t++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							//ConvexHullData
							if(convexHullPolygon.get(t).contains(x,y)){
								hullData [x][y][t] = true;
								convexHullArea [t] += pxArea;
								convexHullxC [t] += (double)(x+xMin);
								convexHullyC [t] += (double)(y+yMin);						
							}else{
								hullData [x][y][t] = false;
							}	
							//Generate Particle Image
							if(data[x][y][t] != 0.0){
								particleImp.getStack().setVoxel(x+xMin,y+yMin,t+tMin,data[x][y][t]);
							}else{
								particleImp.getStack().setVoxel(x+xMin,y+yMin,t+tMin,0.0);
							}
						}
					}
				}
			}	
			particleImp.setDisplayRange(0.0,maxIntensity);
			convexHullPolygon.clear();
			
			if(minimizeImages){
				double transientOutline;
				convexHullImp = IJ.createImage("Convex Hull Image", "8-bit", width+4, height+4, 1, times, 1);
				for(int t = 0; t < times; t++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							if(hullData [x][y][t] == true){
								//Outline
								transientOutline = 0.0;
								//Look for other cell particles in surroundings
								if(y-1>=0){
									if(hullData[x][y-1][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								if(y+1<height){ 
									if(hullData[x][y+1][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								if(x-1>=0){ 
									if(hullData[x-1][y][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								if(x+1<width){ 
									if(hullData[x+1][y][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								//Look for other cell particles in surroundings
								convexHullOutline [t] += transientOutline;
								if(transientOutline!=0.0){
									convexHullImp.getStack().setVoxel(x+2,y+2,t,255.0);
								}else{
									convexHullImp.getStack().setVoxel(x+2,y+2,t,0.0);
								}
							}else{
								convexHullImp.getStack().setVoxel(x+2,y+2,t,0.0);
							}
						}
					}
				}
			}else{
				double transientOutline;
				convexHullImp = IJ.createImage("Convex Hull Image", "8-bit", orWidth, orHeight, 1, orTimes, 1);
				for(int t = 0; t < times; t++){
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							if(hullData [x][y][t] == true){
								//Outline
								transientOutline = 0.0;
								//Look for other cell particles in surroundings
								if(y-1>=0){
									if(hullData[x][y-1][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								if(y+1<height){ 
									if(hullData[x][y+1][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								if(x-1>=0){ 
									if(hullData[x-1][y][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								if(x+1<width){ 
									if(hullData[x+1][y][t]==false){transientOutline+=calibration;}
								}else{transientOutline+=calibration;}
								//Look for other cell particles in surroundings
								convexHullOutline [t] += transientOutline;
								if(transientOutline!=0.0){
									convexHullImp.getStack().setVoxel(x+xMin,y+yMin,t+tMin,255.0);
								}else{
									convexHullImp.getStack().setVoxel(x+xMin,y+yMin,t+tMin,0.0);
								}
							}else{
								convexHullImp.getStack().setVoxel(x+xMin,y+yMin,t+tMin,0.0);
							}
						}
					}
				}
			}			
			convexHullImp.setCalibration(cal);
			
			for(int t = 0; t < times; t++){
				convexHullxC [t] /= (convexHullArea [t]/pxArea);
				convexHullyC [t] /= (convexHullArea [t]/pxArea);
				convexHullxC [t] *= calibration;
				convexHullyC [t] *= calibration;
				convexHullxC [t] += calibration / 2.0;
				convexHullyC [t] += calibration / 2.0;
				
				xPolarityVectorBIN [t] = convexHullxC [t] - xC [t];
				yPolarityVectorBIN [t] = convexHullyC [t] - yC [t];
				polarityVectorLengthBIN [t] = Math.sqrt(Math.pow(xPolarityVectorBIN [t],2.0)+Math.pow(yPolarityVectorBIN[t],2.0));
				polarityIndexBIN [t] = polarityVectorLengthBIN [t]/(2*Math.sqrt(convexHullArea[t]/Math.PI));
				
				xPolarityVectorSemiBIN [t] = convexHullxC [t] - xCOM [t];
				yPolarityVectorSemiBIN [t] = convexHullyC [t] - yCOM [t];
				polarityVectorLengthSemiBIN [t] = Math.sqrt(Math.pow(xPolarityVectorSemiBIN [t],2.0)+Math.pow(yPolarityVectorSemiBIN [t],2.0));
				polarityIndexSemiBIN [t] = polarityVectorLengthSemiBIN [t]/(2*Math.sqrt(convexHullArea[t]/Math.PI));
			}
		}
		
		
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
			shortestPath = new double [times];
			
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
			shortestPathOfAll = new double [times];
						
			ImageStack sklStack = new ImageStack(width+4, height+4);
			if(minimizeImages==false){
				sklStack = new ImageStack(orWidth, orHeight);
			}
			
			ImagePlus particleImpGaussed = (ImagePlus) particleImp.duplicate();
			particleImpGaussed.setCalibration(cal);
			
			if(binarizeBeforeSkeletonization){
				for(int t = 0; t < particleImp.getNFrames(); t++){
					for(int x = 0; x < particleImp.getWidth(); x++){
						for(int y = 0; y < particleImp.getHeight(); y++){
							if(particleImpGaussed.getStack().getVoxel(x,y,t)>0.0){
								particleImpGaussed.getStack().setVoxel(x,y,t,maxPossibIntensity);
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
				IJ.run(particleImpGaussed, "Gaussian Blur...", "sigma=" + gaussformat.format(gaussSigma) + " stack");
//				IJ.run(imp, "Gaussian Blur 3D...", "x=" + gaussformat.format(gaussSigma) + " y=" + gaussformat.format(gaussSigma) + " t=" + 0);				
			// Gaussfilter
				
			ImagePlus tempImp = new ImagePlus();
			ImageStack tempStack; 
			ArrayList<Double> lst;
			
			for(int t = 0; t < times; t++){
				tempStack = new ImageStack(particleImpGaussed.getWidth(),particleImpGaussed.getHeight());
				particleImpGaussed.setSlice(t+1);
				tempStack.addSlice(particleImpGaussed.getProcessor().duplicate());
				tempImp = new ImagePlus();
				tempImp.setStack(tempStack);
				tempImp.setCalibration(cal);
				
//				IJ.run(tempImp,"Skeletonize (2D/3D)","");
				Skeletonize3D_ skelProc = new Skeletonize3D_();
				skelProc.setup("", tempImp);
				skelProc.run(tempImp.getProcessor());

				AnalyzeSkeleton_ skel = new AnalyzeSkeleton_();
				skel.calculateShortestPath = true;
				skel.setup("", tempImp);
				
				//run(int pruneIndex, boolean pruneEnds, boolean shortPath, ImagePlus origIP, boolean silent, boolean verbose)
				SkeletonResult sklRes = skel.run(AnalyzeSkeleton_.NONE, false, true, null, true, false);
				sklStack.addSlice("skeleton " + (t+1), skel.getResultImage(false).getProcessor(1).duplicate());
								
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
				shortestPathOfAll [t] = 0.0;
				
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
					
					for(int i = 0; i < foundSkl [t]; i++){
						sTreeL [i] = sAvBrL[i]*sBranches[i];	//Total Tree length
						if(sTreeL [i] >sklLargestValue){
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
							shortestPathOfAll [t] += lst.get(i); 
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
					shortestPath [t] = lst.get(IDofLargest[t]);
					lst.clear();
					lst.trimToSize();
				}				
			}
						
			skeletonImp = new ImagePlus("Skeleton Image");
			skeletonImp.setStack(sklStack);
			skeletonImp.setCalibration(cal);				
			
			IJ.run(tempImp, "Close", "No");
			IJ.run(particleImpGaussed, "Close", "No");
		}
		System.gc();
		
		//TIME DEPENDENT VARIABLES
		if(times>1){
			deltaArea = new double [times];
			deltaRI = new double [times];
			movingVectorLengthBIN = new double [times];
			movingVectorLengthSemiBIN = new double [times];
			occupArea = new double [times];
			lostArea = new double [times];
			nrOfExtensions = new int [times];
			nrOfRetractions = new int [times];
			nrOfExtensionsMoreThan1Px = new int [times];
			nrOfRetractionsMoreThan1Px = new int [times];
			boolean extensRetractions [][][][] = new boolean [width][height][times][2];	//0 = ext, 1 = retr
			motility = new double [times];
						
			deltaArea [0] = 0.0;
			deltaRI [0] = 0.0;
			movingVectorLengthBIN [0] = 0.0;
			movingVectorLengthSemiBIN [0] = 0.0;
			occupArea [0] = 0.0;
			lostArea [0] = 0.0;
			nrOfExtensions [0] = 0;
			nrOfRetractions [0] = 0;
			nrOfExtensionsMoreThan1Px [0] = 0;
			nrOfRetractionsMoreThan1Px [0] = 0;
			motility [0] = 0.0;
					
			for(int t = 1; t < times; t++){
				deltaArea [t] = area [t] - area [t-1];
				deltaRI [t] = RI [t] - RI [t-1];
				movingVectorLengthBIN [t] = Math.sqrt(Math.pow(xC [t] - xC [t-1],2.0)+Math.pow(yC [t] - yC [t-1], 2.0));
				movingVectorLengthSemiBIN [t] = Math.sqrt(Math.pow(xCOM [t] - xCOM [t-1],2.0)+Math.pow(yCOM [t] - yCOM [t-1], 2.0));
				occupArea[t] = 0.0;
				lostArea[t] = 0.0;
				for(int x = 0; x < width; x++){
					for(int y = 0; y < height; y++){
						if(data [x][y][t] > 0.0 && data [x][y][t-1] == 0.0){
							occupArea [t] += pxArea;
							extensRetractions [x][y][t][0] = true;
						}else{
							extensRetractions [x][y][t][0] = false;
						}
						if(data [x][y][t] == 0.0 && data [x][y][t-1] > 0.0){
							lostArea [t] += pxArea;
							extensRetractions [x][y][t][1] = true;
						}else{
							extensRetractions [x][y][t][1] = false;
						}
					}
				}
				motility [t] = occupArea [t] + lostArea [t];
			}	
			
			//FloodFiller to find #extensions and #retractions
			
			int floodNodeX, floodNodeY, floodNodeT, index, maxExtRetr = 0;
			for(int t = 1; t < times; t++){
				if(maxExtRetr < (int)Math.round(occupArea [t] / pxArea)){
					maxExtRetr = (int)Math.round(occupArea [t] / pxArea);
				}
				if(maxExtRetr < (int)Math.round(lostArea [t] / pxArea)){
					maxExtRetr = (int)Math.round(lostArea [t] / pxArea);
				}				
			}
			int[][] floodNodes = new int[maxExtRetr][3];
			int extensionsCounter, retractionsCounter;
			for(int t = 1; t < times; t++){

				for(int x = 0; x < width; x++){
					for(int y = 0; y < height; y++){
						//extensions flood-filler
						if(extensRetractions[x][y][t][0]){
							extensionsCounter = 1;
							extensRetractions[x][y][t][0] = false;
							
							floodNodeX = x;
							floodNodeY = y;
							floodNodeT = t;
							index = 0;
							
							floodNodes[0][0] = floodNodeX;
							floodNodes[0][1] = floodNodeY;
							floodNodes[0][2] = floodNodeT;
							while (index >= 0){
								floodNodeX = floodNodes[index][0];
								floodNodeY = floodNodes[index][1];
								floodNodeT = floodNodes[index][2];
								index--;            
								if ((floodNodeX > 0) && (extensRetractions[floodNodeX-1][floodNodeY][floodNodeT][0])){
									extensRetractions[floodNodeX-1][floodNodeY][floodNodeT][0] = false;
									
									extensionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeT;
								}
								if ((floodNodeX < (width-1)) && (extensRetractions[floodNodeX+1][floodNodeY][floodNodeT][0])){
									extensRetractions[floodNodeX+1][floodNodeY][floodNodeT][0] = false;
									extensionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeT;
								}
								if ((floodNodeY > 0) && (extensRetractions[floodNodeX][floodNodeY-1][floodNodeT][0])){
									extensRetractions[floodNodeX][floodNodeY-1][floodNodeT][0] = false;
									extensionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeT;
								}                
								if ((floodNodeY < (height-1)) && (extensRetractions[floodNodeX][floodNodeY+1][floodNodeT][0])){
									extensRetractions[floodNodeX][floodNodeY+1][floodNodeT][0] = false;
									extensionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeT;
								}
							}								
							//Filter and save
							nrOfExtensions [t]++;
							if(extensionsCounter > 1){
								nrOfExtensionsMoreThan1Px [t]++;
							}							
						}
						
						//retractions flood-filler
						if(extensRetractions[x][y][t][1]){
							retractionsCounter = 1;
							extensRetractions[x][y][t][1] = false;
							
							floodNodeX = x;
							floodNodeY = y;
							floodNodeT = t; 
							index = 0;
							floodNodes[0][0] = floodNodeX;
							floodNodes[0][1] = floodNodeY;
							floodNodes[0][2] = floodNodeT;
							while (index >= 0){
								floodNodeX = floodNodes[index][0];
								floodNodeY = floodNodes[index][1];
								floodNodeT = floodNodes[index][2];
								index--;            
								if ((floodNodeX > 0) && (extensRetractions[floodNodeX-1][floodNodeY][floodNodeT][1])){
									extensRetractions[floodNodeX-1][floodNodeY][floodNodeT][1] = false;
									
									retractionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeT;
								}
								if ((floodNodeX < (width-1)) && (extensRetractions[floodNodeX+1][floodNodeY][floodNodeT][1])){
									extensRetractions[floodNodeX+1][floodNodeY][floodNodeT][1] = false;
									retractionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeT;
								}
								if ((floodNodeY > 0) && (extensRetractions[floodNodeX][floodNodeY-1][floodNodeT][1])){
									extensRetractions[floodNodeX][floodNodeY-1][floodNodeT][1] = false;
									retractionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeT;
								}                
								if ((floodNodeY < (height-1)) && (extensRetractions[floodNodeX][floodNodeY+1][floodNodeT][1])){
									extensRetractions[floodNodeX][floodNodeY+1][floodNodeT][1] = false;
									retractionsCounter++;
									
									index++;
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeT;
								}
							}								
							//Filter and save
							nrOfRetractions [t]++;
							if(retractionsCounter > 1){
								nrOfRetractionsMoreThan1Px [t]++;
							}							
						}
					}
				}
			}				
		}
		
		//timegroup variables
		projectedTimes = (int)((double)times/(double)projectedFrames);
		boolean particleTouched, hullTouched, particleStatic, hullStatic;
		int gr;
		if(times>=projectedFrames&&projectedFrames!=1){
			projectedArea = new double [projectedTimes];
			projectedStaticArea = new double [projectedTimes];
			projectedDynamicFraction = new double [projectedTimes];
			projectedConvexHullArea = new double [projectedTimes];
			projectedConvexHullStaticArea = new double [projectedTimes];
			projectedConvexHullDynamicFraction = new double [projectedTimes];
			
			for(int i = 0; i < projectedTimes; i++){
				projectedArea [i] = 0.0;
				projectedStaticArea [i] = 0.0;
				projectedConvexHullArea [i] = 0.0;
				projectedConvexHullStaticArea [i] = 0.0;				
			}
			
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					particleTouched = false;
					hullTouched = false;
					particleStatic = true;
					hullStatic = true;
					for(int t = 0; t < times; t++){
						if(data [x][y][t] > 0.0){
							particleTouched = true;
						}else{
							particleStatic = false;
						}
						if(hullData [x][y][t]){
							hullTouched = true;
						}else{
							hullStatic = false;
						}
						
						//Save into group
						if((t+1)%projectedFrames==0){		
							gr = ((int)((double)t/projectedFrames));
							if(particleTouched == true){
								projectedArea [gr] += pxArea;
								particleTouched = false;
							}
							if(hullTouched == true){
								projectedConvexHullArea [gr] += pxArea;
								hullTouched = false;
							}
							if(particleStatic == true){
								projectedStaticArea [gr] += pxArea;
							}else{
								particleStatic = true;
							}
							if(hullStatic == true){
								projectedConvexHullStaticArea [gr] += pxArea;
							}else{
								hullStatic = true;
							}
						}
					}
					
					
				}
			}
			
			for(int i = 0; i < projectedTimes; i++){
				projectedDynamicFraction [i] = (projectedArea [i] - projectedStaticArea [i]) / projectedArea [i];
				projectedConvexHullDynamicFraction [i] = (projectedConvexHullArea [i] - projectedConvexHullStaticArea [i]) / projectedConvexHullArea [i];
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
					x0SemiBIN = xCOM[0], 
					y0SemiBIN = yCOM [0];
			for(int t = 1; t < times; t++){
				accumDistBIN += movingVectorLengthBIN [t];
				accumDistSemiBIN += movingVectorLengthSemiBIN [t];
				if((t+1)%projectedFrames==0){
					gr = ((int)((double)t/projectedFrames));
					accumulatedDistanceBIN [gr] = accumDistBIN;
					euclideanDistanceBIN [gr] = Math.sqrt(Math.pow(x0BIN - xC [t], 2.0)+Math.pow(y0BIN - yC [t], 2.0));
					directionalityIndexBIN [gr] = euclideanDistanceBIN [gr]/ accumulatedDistanceBIN [gr];
					accumulatedDistanceSemiBIN [gr] = accumDistSemiBIN;
					euclideanDistanceSemiBIN [gr] = Math.sqrt(Math.pow(x0SemiBIN - xCOM [t], 2.0)+Math.pow(y0SemiBIN - yCOM [t], 2.0));
					directionalityIndexSemiBIN [gr] = euclideanDistanceSemiBIN [gr]/ accumulatedDistanceSemiBIN [gr];
					
					accumDistBIN = 0.0;	
					accumDistSemiBIN = 0.0;
					if(t+1<times){
						x0BIN = xC[t+1]; 
						y0BIN = yC[t+1]; 
						x0SemiBIN = xCOM[t+1];
						y0SemiBIN = yCOM [t+1];
					}
				}
			}
		}
		
		//LONG TERM AVERAGES OF PARAMETERS
		if(times>=projectedFrames&&projectedFrames!=1){
			avXC = new double [projectedTimes];
			avYC = new double [projectedTimes]; 
			avXCOM = new double [projectedTimes];
			avYCOM = new double [projectedTimes];
			avXSpan = new double [projectedTimes];
			avYSpan = new double [projectedTimes];
			avAverageIntensity = new double [projectedTimes];
			avMinimumIntensity = new double [projectedTimes];
			avMaximumIntensity = new double [projectedTimes];
			avSdIntensity = new double [projectedTimes];
			avArea = new double [projectedTimes]; 
			avOutline = new double [projectedTimes];
			avRI = new double [projectedTimes];
			avConvexHullArea = new double [projectedTimes];
			avConvexHullOutline = new double [projectedTimes];
			avConvexHullxC = new double [projectedTimes];
			avConvexHullyC = new double [projectedTimes];
			avXPolarityVectorBIN = new double [projectedTimes];
			avYPolarityVectorBIN = new double [projectedTimes];
			avPolarityVectorLengthBIN = new double [projectedTimes];
			avPolarityIndexBIN = new double [projectedTimes]; 
			avXPolarityVectorSemiBIN = new double [projectedTimes];
			avYPolarityVectorSemiBIN = new double [projectedTimes];
			avPolarityVectorLengthSemiBIN = new double [projectedTimes];
			avPolarityIndexSemiBIN = new double [projectedTimes];
			
			for(int t = 0; t < times-times%projectedFrames; t++){
				gr = ((int)((double)t/projectedFrames));
				avXC [gr] += xC [t];
				avYC [gr] += yC [t]; 
				avXCOM [gr] += xCOM [t]; 
				avYCOM [gr] += yCOM [t]; 
				avXSpan [gr] += xSpan [t]; 
				avYSpan [gr] += ySpan [t]; 
				avAverageIntensity [gr] += averageIntensity [t]; 
				avMinimumIntensity [gr] += minimumIntensity [t]; 
				avMaximumIntensity [gr] += maximumIntensity [t]; 
				avSdIntensity [gr] += sdIntensity [t];
				avArea [gr] += area [t]; 
				avOutline [gr] += outline [t]; 
				avRI [gr] += RI [t]; 
				avConvexHullArea [gr] += convexHullArea [t]; 
				avConvexHullOutline [gr] += convexHullOutline [t]; 
				avConvexHullxC [gr] += convexHullxC [t]; 
				avConvexHullyC [gr] += convexHullyC [t]; 
				avXPolarityVectorBIN [gr] += xPolarityVectorBIN [t]; 
				avYPolarityVectorBIN [gr] += yPolarityVectorBIN [t]; 
				avPolarityVectorLengthBIN [gr] += polarityVectorLengthBIN [t]; 
				avPolarityIndexBIN [gr] += polarityIndexBIN [t];  
				avXPolarityVectorSemiBIN [gr] += xPolarityVectorSemiBIN [t]; 
				avYPolarityVectorSemiBIN [gr] += yPolarityVectorSemiBIN [t]; 
				avPolarityVectorLengthSemiBIN [gr] += polarityVectorLengthSemiBIN [t]; 
				avPolarityIndexSemiBIN [gr] += polarityIndexSemiBIN [t]; 
			}
			for(int group = 0; group < projectedTimes; group++){
				avXC [group] /= (double) projectedFrames;
				avYC [group] /= (double) projectedFrames; 
				avXCOM [group] /= (double) projectedFrames; 
				avYCOM [group] /= (double) projectedFrames;
				avXSpan [group] /= (double) projectedFrames;
				avYSpan [group] /= (double) projectedFrames;
				avAverageIntensity [group] /= (double) projectedFrames;
				avMinimumIntensity [group] /= (double) projectedFrames;
				avMaximumIntensity [group] /= (double) projectedFrames;
				avSdIntensity [group] /= (double) projectedFrames;
				avArea [group] /= (double) projectedFrames;
				avOutline [group] /= (double) projectedFrames; 
				avRI [group] /= (double) projectedFrames;
				avConvexHullArea [group] /= (double) projectedFrames;
				avConvexHullOutline [group] /= (double) projectedFrames;
				avConvexHullxC [group] /= (double) projectedFrames;
				avConvexHullyC [group] /= (double) projectedFrames;
				avXPolarityVectorBIN [group] /= (double) projectedFrames; 
				avYPolarityVectorBIN [group] /= (double) projectedFrames;
				avPolarityVectorLengthBIN [group] /= (double) projectedFrames;
				avPolarityIndexBIN [group] /= (double) projectedFrames;
				avXPolarityVectorSemiBIN [group] /= (double) projectedFrames;
				avYPolarityVectorSemiBIN [group] /= (double) projectedFrames;
				avPolarityVectorLengthSemiBIN [group] /= (double) projectedFrames; 
				avPolarityIndexSemiBIN [group] /= (double) projectedFrames;
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
				avShortestPath = new double [projectedTimes];
				avTreeLengthOfAll = new double [projectedTimes];
				avAvBranchLengthOfAll = new double [projectedTimes];
				avMaxBranchLengthOfAll = new double [projectedTimes];
				avShortestPathOfAll = new double [projectedTimes];
				
				for(int t = 0; t < times-times%projectedFrames; t++){
					int group = ((int)((double)t/projectedFrames));
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
					avShortestPath [group] += 0.0 + shortestPath [t];
					avTreeLengthOfAll [group] += 0.0 + treeLengthOfAll [t];
					avAvBranchLengthOfAll [group] += 0.0 + avBranchLengthOfAll [t];
					avMaxBranchLengthOfAll [group] += 0.0 + maxBranchLengthOfAll [t];
					avShortestPathOfAll [group] += 0.0 + shortestPathOfAll [t];
				}
				for(int group = 0; group < projectedTimes; group++){
					avFoundSkl [group] /= (double) projectedFrames;
					avBranches [group] /= (double) projectedFrames;
					avJunctions [group] /= (double) projectedFrames;
					avTips [group] /= (double) projectedFrames;
					avTriplePs [group] /= (double) projectedFrames;
					avQuadruplePs [group] /= (double) projectedFrames;
					avJunctionVx [group] /= (double) projectedFrames;
					avSlabVx [group] /= (double) projectedFrames;
					avBranchesOfAll [group] /= (double) projectedFrames;
					avJunctionsOfAll [group] /= (double) projectedFrames;
					avTipsOfAll [group] /= (double) projectedFrames;
					avTriplePsOfAll [group] /= (double) projectedFrames;
					avQuadruplePsOfAll [group] /= (double) projectedFrames;
					avJunctionVxOfAll [group] /= (double) projectedFrames;
					avSlabVxOfAll [group] /= (double) projectedFrames;
					avTreeLength [group] /= (double) projectedFrames;
					avAvBranchLength [group] /= (double) projectedFrames;
					avMaxBranchLength [group] /= (double) projectedFrames;
					avShortestPath [group] /= (double) projectedFrames;
					avTreeLengthOfAll [group] /= (double) projectedFrames;
					avAvBranchLengthOfAll [group] /= (double) projectedFrames;
					avMaxBranchLengthOfAll [group] /= (double) projectedFrames;
					avShortestPathOfAll [group] /= (double) projectedFrames;
				}
			}
			
			//Dynamic parameters
			if(times>1){
				avMovingVectorLengthBIN = new double [projectedTimes];
				avMovingVectorLengthSemiBIN = new double [projectedTimes];
				avOccupArea = new double [projectedTimes];
				avLostArea = new double [projectedTimes];
				avMotility = new double [projectedTimes];
				avDeltaArea = new double [projectedTimes];
				avDeltaRI = new double [projectedTimes];
				
				avNrOfExtensions = new double [projectedTimes];
				avNrOfRetractions = new double [projectedTimes];
				avNrOfExtensionsMoreThan1Px = new double [projectedTimes];
				avNrOfRetractionsMoreThan1Px = new double [projectedTimes];
				
				for(int t = 0; t < times-times%projectedFrames; t++){
					gr = ((int)((double)t/projectedFrames));
					avMovingVectorLengthBIN [gr] += movingVectorLengthBIN [t];
					avMovingVectorLengthSemiBIN [gr] += movingVectorLengthSemiBIN [t];
					avOccupArea [gr] += occupArea [t];
					avLostArea [gr] += lostArea [t];
					avMotility [gr] += motility [t];
					avDeltaArea [gr] += deltaArea [t];
					avDeltaRI [gr] += deltaRI [t];	
					
					avNrOfExtensions [gr] += nrOfExtensions [t];
					avNrOfRetractions [gr] += nrOfRetractions [t];
					avNrOfExtensionsMoreThan1Px [gr] += nrOfExtensionsMoreThan1Px [t];
					avNrOfRetractionsMoreThan1Px [gr] += nrOfRetractionsMoreThan1Px [t];
					
					if((t+1)%projectedFrames==0){	
						t++;	//Shift induces every averaged value to emerge from the same number of values included into calculation
					}
				}
				
				for(int group = 0; group < projectedTimes; group++){
					avMovingVectorLengthBIN [group] /= ((double) projectedFrames - 1.0);
					avMovingVectorLengthSemiBIN [group] /= ((double) projectedFrames - 1.0);
					avOccupArea [group] /= ((double) projectedFrames - 1.0);
					avLostArea [group] /= ((double) projectedFrames - 1.0);
					avMotility [group] /= ((double) projectedFrames - 1.0);
					avDeltaArea [group] /= ((double) projectedFrames - 1.0);
					avDeltaRI [group] /= ((double) projectedFrames - 1.0);
					avNrOfExtensions [group] /= ((double) projectedFrames - 1.0);
					avNrOfRetractions [group] /= ((double) projectedFrames - 1.0);
					avNrOfExtensionsMoreThan1Px [group] /= ((double) projectedFrames - 1.0);
					avNrOfRetractionsMoreThan1Px [group] /= ((double) projectedFrames - 1.0);					
				}
			}		
		}
		initialized = true;
	}
	
	public void closeImps (){
		if(branches!=null){
			skeletonImp.changes = false;
			skeletonImp.close();
		}	
		particleImp.changes = false;
		particleImp.close();
		convexHullImp.changes = false;
		convexHullImp.close();
	}
}