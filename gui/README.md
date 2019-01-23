CRISTAL-iSE GUI [![Build Status](https://travis-ci.org/cristal-ise/gui.svg?branch=master)](https://travis-ci.org/cristal-ise/gui)
===============

An administrative UI for CRISTAL, using Swing.

The default UI provides a low-level view on Items. It features:

- Browsing and searching of the domain tree
- Overview of Item properties, which for administrators allows changes via calls to the WriteProperty predefined step
- Graphical Workflow view, including execution of outcome-less transitions. Direct state override and workflow editing for administrators.
- Activity execution, including for transitions requiring outcomes. Outcomes are rendered according to schema:
  - Composite Activity Definitions using the graphical workflow viewer.
  - Elementary Activity Definitions in table form
  - Scripts and Schemas as raw XML
  - All other schemas attempt to use a limited form generator which tries to construct forms from XML Schemas.
- Browsing Outcome data by Schema, Viewpoint and Event. Administrative re-assignment of viewpoints.
