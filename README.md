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
MotiQ is a set of ImageJ plugins that can be run in ImageJ or the extended ImageJ version FIJI. Thus to use MotiQ you need to first have an ImageJ or FIJI software downloaded to your computer. ImageJ/FIJI are open-source, freely available softwares that can be downloaded [here](https://imagej.net/downloads). MotiQ's plugins can be installed either (A) via the Update Manager in ImageJ/FIJI (recommend) or (B) manually (if you don't have internet access fromy our FIJI/ImageJ2 or use a basic ImageJ installation, which does not contain the update manager).

#### (A) Installing MotiQ via the Update Manager
Since December 2022, MotiQ can be simply installed through FIJIs update manager. This also ensures that you always get the latest MotiQ release when you update your FIJI / ImageJ2. Pursue the installation as follows:

- Go to the menu entry Help > Update in your FIJI. 
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205484552-b4161f8d-e4e6-4513-8d20-a5f5f3791cf6.png" width=400>
</p>

- A dialog pops up that loads update site information - wait for it to complete.
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205484622-704c0546-ae06-4858-8b0d-89be969f5770.png" width=300>
</p>

- When it completed it will automatically close and open the following dialog (note that if you haven't updated your FIJI for a long time here may be a lot of updates listed - this is no problem you can just update everything along with installing CiliaQ so just continue with the following descriptions).

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205484704-8786306c-7ce0-41d0-8967-167a7c461cbc.png" width=500>
</p>

- Press OK in the "Your ImageJ is up to date"-message (if the message is displayed)

- Click "Manage Update Sites". This will open another dialog, in which you need to check the MotiQ update site (you find it by scrolling down). Afterwards, press "Close".

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205502611-15a0a736-a7cb-474a-818e-dff0b10db04b.png" width=600>
</p>

- Now, in the original update dialog you will see new modules added for installation. Click "apply changes".

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205502671-b5fb19f1-8257-4589-b5c8-879b59cbb209.png" width=400>
</p>

- The installation process may take some time, depending on the download speed. The installation process terminates with the following dialog:
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205484904-7c6cc745-28d9-449e-8c8d-8bae4bb064c7.png" width=400>
</p>

- Close FIJI and restart FIJI. 

- You can now verify that the "MotiQ" are installed. 
   - To check that MotiQ is installed, verify that the menu entry Plugins > MotiQ is available.

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205502987-3b8d7a4a-f9d0-4881-8b66-733172879c03.png" width=450>
</p>


#### (B) Installing MotiQ to a pure ImageJ / installing MotiQ manually
Use these installation instructions if ...
- you use a pure ImageJ, which does not include the update manager
- you cannot access the internet from your FIJI/ImageJ distribution
- you do not want to use the update manager

Perform the installation as follows:
- Download the MotiQ plugins (only the .jar files are needed) from the latest release at the [release page](https://github.com/hansenjn/MotiQ/releases).

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205503329-c1f43696-69ac-40cb-962a-4ce310914812.PNG" width=500>
</p>

- Launch ImageJ and install the plugins by drag and drop into the ImageJ window (red marked region in the screenshot below) 
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/201358020-c3685947-b5d8-4127-88ec-ce9b4ddf0e56.png" width=500>
</p>

- Confirm the installations by pressing save in the upcoming dialog(s).
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205503560-8f951dd9-9ef7-4723-a6bc-225496a35168.png" width=500>
</p>

- Next, ImageJ requires to be restarted (close it and start it again)

- You can now verify that the "MotiQ" are installed. 
   - To check that MotiQ is installed, verify that the menu entry Plugins > MotiQ is available.

<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/205502987-3b8d7a4a-f9d0-4881-8b66-733172879c03.png" width=450>
</p>

### Applying MotiQ
Donwload a manual from this repository [here](https://github.com/hansenjn/MotiQ/blob/master/Manual/MotiQ_Manual_v2021-1.pdf). Example settings can also be found in the original MotiQ publication. Note that MotiQ settings may require to be optimized for your data set before MotiQ can deliver perfect results.
