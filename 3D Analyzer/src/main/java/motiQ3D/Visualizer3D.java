/***===============================================================================
 * 
 * MotiQ_3D Version plugin for ImageJ, Version v0.1.5
 * 
 * Copyright (C) 2014-2017 Jan Niklas Hansen
 * First version: July 28, 2014  
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
 * ===========================================================================**/

package motiQ3D;

import java.awt.Color;

import ij.ImagePlus;
import motiQ3D.volumeViewer3D.Volume_Viewer;

public class Visualizer3D{
	ImagePlus imp;
	private boolean showTF = false;
	private int displayMode = 4;
	private int interpolation = 1;	//nearest (0), trilinear (1), tricubic smooth (2), tricubic sharp (3)
	private Color background = Color.white;
	private int lut = 0; //"lut=0-4"
	private int sampling = 1;	//"sampling>0"
	private float distance = -290.0f;	//"dist"	//Float
	private boolean showAxes = false,	//show Axes
		showSlice = false,	//show Slice
		showClipLines = false; // show ClipLines
	private float scale = 5.0f,	//scale
		angleX = 0.0f,	//angleX: float 0-360
		angleY = 0.0f,	//angleY: float 0-360
		angleZ = 0.0f;	//angleZ: float 0-360
	private int alphaMode = 0,	//alpha mode 0-3
		width = 600,	//width
		height = 400;	//height
	private boolean useLight = true; 	//boolean useLight,
	private float ambient = (float) 0.2,	//ambientValue, //float 0-1
		diffuse = (float) 0.1,	//diffuseValue, //float 0-1
		specular = (float) 0.0,	//specularValue, //float 0-1
		shine = (float) 0.0,		//shineValue, //float 0-200
		objectLight = (float) 1.2;	//float objectLightValue, float 0-2
	private int lightRed = 255,	//int lightRed,
		lightGreen = 255,	//int lightGreen,
		lightBlue = 255;	//int lightBlue,
	private float lightPosX = -0.25f,	// light position x (-1 to 1)
		lightPosY = 0.0f;	// light position y (-1 to 1)
	int alphaOffset1 = -50;	//-150 to +150
	
	/**
	 * Generates a Visualizer3D for the ImagePlus
	 * @param imp to be visualized.
	 * @param scale defines the magnification of the 3D visualization
	 * */
	public Visualizer3D(ImagePlus imp, float scale){
		this.imp = imp;
		setScale(scale);
	}
	
	/**
	 * sets the ImagePlus to be visualized to
	 * @param imp
	 * and updates width, height, and scale of the visualisation image  
	 * */	
	public void setImage(ImagePlus imp){
		this.imp = imp;
		setScale(scale);
	}
	
	/**
	 * sets the scale of the visualisation to 
	 * @param scale
	 * and updates width and height of the visualisation image  
	 * */
	public void setScale(float scale){
		this.scale = scale;
		this.width = 50 + (int)Math.round(Math.sqrt(Math.pow(scale * imp.getWidth(), 2.0) 
				+ Math.pow(scale * imp.getHeight(), 2.0)
				+ Math.pow(scale * imp.getNSlices() / ((imp.getCalibration().pixelWidth
						+ imp.getCalibration().pixelHeight) / imp.getCalibration().pixelDepth), 2.0)));
		this.height = this.width;
	}
	
	/**
	 * @param interpolation = 0-4: nearest (0), trilinear (1), tricubic smooth (2), tricubic sharp (3)
	 * */
	public void setInterpolation(int interpolation) {
		this.interpolation = interpolation;
	}
	
	/**
	 * @param lightPosX: (-1 to 1)
	 * */
	public void setLightPosX(float lightPosX) {
		this.lightPosX = lightPosX;
	}
	
	/**
	 * @param lightPosY: (-1 to 1)
	 * */
	public void setLightPosY(float lightPosY) {
		this.lightPosY = lightPosY;
	}
	
	/**
	 * @param objectLightValue: (float: 0.0f to 2.0f)
	 * */
	public void setObjectLightValue(float objectLightValue) {
		this.objectLight = objectLightValue;
	}
	
	/**
	 * @param alphaOffset1: (float: 0.0f to 2.0f)
	 * */
	public void setAlphaOffset1(int alphaOffset1) {
		this.alphaOffset1 = alphaOffset1;
	}
	
	/**
	 * @param angleX: float (0 to 360)
	 * @param angleY: float (0 to 360)
	 * @param angleZ: float (0 to 360)
	 * */
	public void setAngle(float angleX, float angleY, float angleZ) {
		this.angleX = angleX;
		this.angleY = angleY;
		this.angleZ = angleZ;
	}
	
	/**
	 * @return ImagePlus containing a 3D visualization
	 * */
	public ImagePlus get3DVisualization(){
		Volume_Viewer vv = new Volume_Viewer();
		return vv.get3DVisualization(imp, showTF, displayMode, interpolation, background,
				lut, sampling, distance, showAxes, showSlice, showClipLines, scale,
				angleX, angleY, angleZ, alphaMode, width, height,
				useLight, ambient, diffuse, specular, shine,
				objectLight, lightRed, lightGreen, lightBlue, 
				lightPosX, lightPosY, alphaOffset1);
	}

	/**
	 * @return width of the output 3D Visualization
	 * */
	public int getWidth(){
		return width;
	}
	
	/**
	 * @return height of the output 3D Visualization
	 * */
	public int getHeight(){
		return height;
	}
}
