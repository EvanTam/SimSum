About SimSum
============

This program measures the similarity between a user query and the retrieved webpage results from a internet search engine
such as Google or Bing. This algorithm considers the meaning between words rather than relying on exact word match. For
example this algorithm will output a high score for the query "Car" and a webpage that contains the following words: engine,
wheel, window, glass, plastic, metal. The reason is that those words are the components and makeup of a car.

This program uses the following toolboxes which are not part of my work:
Stanford POS tagger
Java Wordnet Interface 2.3.0
Boilerpipe 1.2.0

This program is based on the semantic distance measuring algorithm from my PhD thesis:
King Yiu Tam, "Video Summarisation Based on Speaker Unit", PhD thesis, University of Sydney, 2011

How to use this program
=======================

This program is designed as four classes/modules working in a pipeline. The input and output of these modules are in the form
of text files. The reason for this design is because it will be reimplemented with a MapReduce framework such as Hadoop in
the future. To test this program do the following:

1. Clone this repository into your Eclipse workspace.
2. Make sure you are able to access the internet.
3. Compile and run the class WithoutMapReduce.java

My contact
==========

If you have any questions or comments feel free to email me:
evantam (AT) hotmail (DOT) com
