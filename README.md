# TensorFlow food detection on Android

## Introduction
Object detection on Android have become possible with smartphones of a new generation but still there are a lot of problems associated with real time recognition on mobile platform.
This project is aimed to create an android application with several classifiers included which are capable to detect various types of food. In fact, our app is flexible to applying different models to it.

## Algorithm
We use a binary classifier on every camera frame to determine weather it is food or not. If binary classifier consistenly gives out a "food" result we select one of the latest image with the best score. Then, there are two ways to recognize food type:
* Server:
Python server receives compressed image from device, recognize it using neural network and then send a responce. This option works only with internet connection and may be turned off in the settings menu.
* On device recognition:
To use this option you have to download model you need from our cloud (it is possible to do right from the settings menu). It works slower than server recognition for majority of devices but gives you opportunity to use the model offline.
