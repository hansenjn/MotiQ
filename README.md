# MotiQ
An set of ImageJ plugins to quantify dynamics, morphology, and fluorescence of microglial (or other) cells.

## Included tools
- **MotiQ_Cropper**: An ImageJ plugin to efficiently select individual cells from single images, time-lapse images, static 3D images, or time-lapse 3D images.
- **MotiQ_Thresholder**: An ImageJ plugin to segment images for later quantification by *MotiQ 2D Analyzer* or *MotiQ 3D Analyzer*.
- **MotiQ_2D_Analyzer**: An ImageJ plugin to quantify the output images from *MotiQ Thresholder* in 2D.
- **MotiQ_3D_Analyzer**: An ImageJ plugin to quantify the output images from *MotiQ Thresholder* in 3D.

## How to cite?
When using any of the MotiQ plugins, please cite MotiQ by referencing its publication:

Jan N. Hansen, Matthias Brückner, Marie J. Pietrowski, Jan F. Jikeli, Monika Plescher, Hannes Beckert, Mareike Schnaars, Lorenz Fülle, Katharina Reitmeier, Thomas Langmann, Irmgard Förster, Delphine Boche, Gabor C. Petzold, and Annett Halle. MotiQ: an open-source toolbox to quantify the cell motility and morphology of microglia. *Molecular biology of the cell*, 33:11 (**2022**). https://doi.org/10.1091/mbc.E21-11-0585

## Copyright notice and contacts
Copyright (C) 2014-2022: Jan N. Hansen. 

Contact: jan.hansen (at) uni-bonn.de

## How to use MotiQ?
### Installing MotiQ
MotiQ is a set of ImageJ plugins that can be run in ImageJ or the extended ImageJ version FIJI. Thus to use MotiQ you need to first have an ImageJ or FIJI software downloaded to your computer. ImageJ/FIJI are open-source, freely available softwares that can be downloaded [here](https://imagej.net/downloads).

In the next step, download the MotiQ plugins (only the .jar files are needed) from the latest release at the [release page](https://github.com/hansenjn/MotiQ/releases).

To install the plugins, launch your ImageJ or FIJI and drag and drop the .jar files that you downloaded into ImageJs/FIJIs status bar (this is the red marked region in the following image).

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/201358020-c3685947-b5d8-4127-88ec-ce9b4ddf0e56.png" width=500>
</p>

Next, dialogs will pop up to save the plugins, just confirm those. FIJI/ImageJ will ask you to restart the software. Close the software and open it again. Now, the installed plugins should be available from the FIJI/ImageJ menu at Plugins > MotiQ > ...

### How to use MotiQ
Donwload a manual from this repository [here](https://github.com/hansenjn/MotiQ/blob/master/Manual/MotiQ_Manual_v2021-1.pdf). Example settings can also be found in the original MotiQ publication. Note that MotiQ settings may require to be optimized for your data set before MotiQ can deliver perfect results.
