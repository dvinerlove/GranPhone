Title: Touch Target Sizes

URL Source: https://www.lukew.com/ff/entry.asp?1085

Final URL: https://www.lukew.com/ff/1085/touch-target-sizes

Published Time: 2010-05-04T00:00:00.000Z

Markdown Content:
People interact with touch-based user interfaces with their fingers. So user interface controls have to be big enough to capture fingertip actions without frustrating users with erroneous actions and tiny targets. Ok, so how big?

![Image 1: touch target sizes](https://static.lukew.com/touchtargetsizes.png)In the [iPhone Human Interface Guidelines](http://developer.apple.com/iphone/library/documentation/UserExperience/Conceptual/MobileHIG/DesigningNativeApp/DesigningNativeApp.html#//apple_ref/doc/uid/TP40006556-CH4-SW1), Apple recommends a minimum target size of 44 pixels wide 44 pixels tall. Since physical pixel size can vary by screen density, Apple's pixel specifications apply best to the iPhone's 320 by 480 pixel, 3.5 inch display (164ppi). Since the release of the iPhone 4's [Retina Display](https://www.lukew.com/ff/1142/designing-for-the-retina-display-326ppi) (326ppi) Apple has updated these specs to points instead of pixels.

In the [Windows Phone UI Design and Interaction Guide](http://go.microsoft.com/?linkid=9713252) (PDF), Microsoft goes further and suggests: a recommended touch target size of 9mm/34px; a minimum touch target size of 7mm/26px; a minimum spacing between elements of 2mm/8px; and the visual size of a UI control to be 60-100% of the touch target size.

They also suggest touch targets can be larger than 9mm if: the UI element is frequently touched; the result of a touch error is severe or really frustrating; the UI element is located toward edge of the screen or difficult to hit; or when the UI element is part of a sequential task –like using the dial pad.

[Nokia's developer resources](http://library.forum.nokia.com/index.jsp?topic=/S60_5th_Edition_Cpp_Developers_Library/GUID-5486EFD3-4660-4C19-A007-286DE48F6EEF.html) suggest that touchable interface elements should not be smaller than the smallest average finger pad, that is, no smaller than 1 cm (0.4") in diameter or a 1 cm × 1 cm square. Minimum target sizes for a finger usable UI element are:

*   7 x 7 mm with 1 mm gaps for index finger usage
*   8 x 8 mm with 2 mm gaps for thumb usage
*   List type of components should have minimum of 5 mm line spacing
*   The width of a finger limits the density of items on screen. If the items are too close, the user will not be able to choose a single one.

The [Ubuntu Designing for Finger UIs](https://help.ubuntu.com/community/UMEGuide/DesigningForFingerUIs) documentation states the minimum size of buttons and other interface elements should determined by the size of an adult finger (diameter of 16mm to 20mm). When interacting with a touchscreen, users will prefer to use the pad of their finger rather than the very tip. The pad of the finger is slightly narrower than the full width of the finger: 10-14mm. The fingertip is smaller still - 8-10mm wide - but more awkward to use than the pad of the finger. In general, interface elements should be no smaller than 1cm (0.4"). Minimum button size for a finger-driven UI is a function of pixel density, not screen resolution.

A study of [One-Handed Thumb Use on Small Touchscreen Devices](http://hcil.cs.umd.edu/trs/2006-11/2006-11.htm) (2005) that looked in detail at the interaction between target size and task performance in single- and multi-target tasks recommended target sizes at least 9.2 mm for single-target tasks and 9.6 mm for multi-target tasks.

An MIT Touch Lab study of [Human Fingertips to Investigate the Mechanics of Tactile Sense](http://touchlab.mit.edu/publications/2003_009.pdf) (PDF) found that the average human finger pad is 10-14mm and the average fingertip is 8-10mm.

Know of any other resources for touch target sizes? Send them [my way](http://www.lukew.com/about/)!
