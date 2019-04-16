/***===============================================================================
 * 
 * MotiQ_3D Version plugin for ImageJ, Version v0.1.3
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
 * For any questions please feel free to contact me (jan.hansen@caesar.de).
 * 
 * ===========================================================================**/

package motiQ3D;

public class Point3D{
	public int x = 0,
		y = 0,
		z = 0;
	public Point3D(int ix, int iy, int iz){
		x = 0 + ix;
		y = 0 + iy;
		z = 0 + iz;
	}	
}