/***===============================================================================
 *  
 * MotiQ_2D plugin for imageJ
 * 
 * Copyright (C) 2014-2024 Jan N. Hansen
 * First version: November 07, 2014   
 * This Version: July 31, 2024
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class outputTextFile {
	String path;	
	FileWriter writer = null;
	
	public outputTextFile(String outputPath) {
		path = outputPath;		
		try {
			writer = new FileWriter(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void append(String text) {
		try {
			writer.write(text + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void finish() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
