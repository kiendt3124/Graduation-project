package com.uet.expensetracker.ui.theme

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

object MyIcons {
    val Group: ImageVector
        get() {
            if (_group != null) return _group!!
            _group = materialIcon(name = "Filled.Group") {
                materialPath {
                    moveTo(16.0f, 11.0f)
                    curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f)
                    reflectiveCurveTo(17.66f, 5.0f, 16.0f, 5.0f)
                    curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                    reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                    close()
                    moveTo(8.0f, 11.0f)
                    curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f)
                    reflectiveCurveTo(9.66f, 5.0f, 8.0f, 5.0f)
                    curveTo(6.34f, 5.0f, 5.0f, 6.34f, 5.0f, 8.0f)
                    reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                    close()
                    moveTo(8.0f, 13.0f)
                    curveToRelative(-2.33f, 0.0f, -7.0f, 1.17f, -7.0f, 3.5f)
                    verticalLineTo(19.0f)
                    horizontalLineToRelative(14.0f)
                    verticalLineToRelative(-2.5f)
                    curveToRelative(0.0f, -2.33f, -4.67f, -3.5f, -7.0f, -3.5f)
                    close()
                    moveTo(16.0f, 13.0f)
                    curveToRelative(-0.29f, 0.0f, -0.62f, 0.02f, -0.97f, 0.05f)
                    curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
                    verticalLineTo(19.0f)
                    horizontalLineToRelative(6.0f)
                    verticalLineToRelative(-2.5f)
                    curveToRelative(0.0f, -2.33f, -4.67f, -3.5f, -7.0f, -3.5f)
                    close()
                }
            }
            return _group!!
        }

    val BusinessCenter: ImageVector
        get() {
            if (_businessCenter != null) return _businessCenter!!
            _businessCenter = materialIcon(name = "Filled.BusinessCenter") {
                materialPath {
                    moveTo(10.0f, 16.0f)
                    verticalLineToRelative(-1.0f)
                    horizontalLineTo(3.01f)
                    lineTo(3.0f, 21.0f)
                    horizontalLineToRelative(18.0f)
                    verticalLineToRelative(-6.0f)
                    horizontalLineToRelative(-7.0f)
                    verticalLineToRelative(1.0f)
                    horizontalLineToRelative(-4.0f)
                    close()
                    moveTo(20.0f, 7.0f)
                    horizontalLineToRelative(-4.01f)
                    verticalLineTo(5.0f)
                    lineToRelative(-2.0f, -2.0f)
                    horizontalLineToRelative(-4.0f)
                    lineToRelative(-2.0f, 2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(2.0f)
                    verticalLineToRelative(6.0f)
                    horizontalLineToRelative(20.0f)
                    verticalLineTo(7.0f)
                    close()
                    moveTo(14.0f, 7.0f)
                    horizontalLineToRelative(-4.0f)
                    verticalLineTo(5.0f)
                    horizontalLineToRelative(4.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }
            return _businessCenter!!
        }

    val Schedule: ImageVector
        get() {
            if (_schedule != null) return _schedule!!
            _schedule = materialIcon(name = "Filled.Schedule") {
                materialPath {
                    moveTo(11.99f, 2.0f)
                    curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
                    curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                    reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
                    close()
                    moveTo(12.0f, 20.0f)
                    curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
                    reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
                    reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
                    reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
                    close()
                    moveTo(12.5f, 7.0f)
                    horizontalLineTo(11.0f)
                    verticalLineToRelative(6.0f)
                    lineToRelative(5.25f, 3.15f)
                    lineToRelative(0.75f, -1.23f)
                    lineToRelative(-4.5f, -2.67f)
                    close()
                }
            }
            return _schedule!!
        }

    // Using AddPhotoAlternate as the closest match for the image+ icon
    val AddPhotoAlternate: ImageVector
        get() {
            if (_addPhotoAlternate != null) return _addPhotoAlternate!!
            _addPhotoAlternate = materialIcon(name = "Filled.AddPhotoAlternate") {
                materialPath {
                    moveTo(18.0f, 20.0f)
                    lineTo(4.0f, 20.0f)
                    lineTo(4.0f, 6.0f)
                    horizontalLineToRelative(9.0f)
                    lineTo(13.0f, 4.0f)
                    lineTo(4.0f, 4.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    lineTo(20.0f, 10.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(10.0f)
                    close()
                    moveTo(13.0f, 12.0f)
                    lineTo(11.0f, 14.55f)
                    lineTo(9.0f, 11.8f)
                    lineTo(6.0f, 16.0f)
                    horizontalLineToRelative(10.0f)
                    lineToRelative(-3.0f, -4.0f)
                    close()
                }
                materialPath {
                    moveTo(19.0f, 3.0f)
                    lineTo(19.0f, 1.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    lineTo(19.0f, 5.0f)
                    horizontalLineToRelative(2.0f)
                    lineTo(21.0f, 3.0f)
                    close()
                }
            }
            return _addPhotoAlternate!!
        }

    val PhotoCamera: ImageVector
        get() {
            if (_photoCamera != null) return _photoCamera!!
            _photoCamera = materialIcon(name = "Filled.PhotoCamera") {
                materialPath {
                    moveTo(12.0f, 12.0f)
                    moveToRelative(-3.2f, 0.0f)
                    arcToRelative(3.2f, 3.2f, 0.0f, true, true, 6.4f, 0.0f)
                    arcToRelative(3.2f, 3.2f, 0.0f, true, true, -6.4f, 0.0f)
                }
                materialPath {
                    moveTo(9.0f, 2.0f)
                    lineTo(7.17f, 4.0f)
                    lineTo(4.0f, 4.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(12.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(16.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    lineTo(22.0f, 6.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineToRelative(-3.17f)
                    lineTo(15.0f, 2.0f)
                    lineTo(9.0f, 2.0f)
                    close()
                    moveTo(12.0f, 17.0f)
                    curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                    reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                    reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                    reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                    close()
                }
            }
            return _photoCamera!!
        }

    val CheckBoxOutlineBlank: ImageVector
        get() {
            if (_checkBoxOutlineBlank != null) return _checkBoxOutlineBlank!!
            _checkBoxOutlineBlank = materialIcon(name = "Filled.CheckBoxOutlineBlank") {
                materialPath {
                    moveTo(19.0f, 5.0f)
                    verticalLineToRelative(14.0f)
                    horizontalLineTo(5.0f)
                    verticalLineTo(5.0f)
                    horizontalLineToRelative(14.0f)
                    moveToRelative(0.0f, -2.0f)
                    horizontalLineTo(5.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(5.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                }
            }
            return _checkBoxOutlineBlank!!
        }
}

// Backing fields
private var _group: ImageVector? = null
private var _businessCenter: ImageVector? = null
private var _schedule: ImageVector? = null
private var _addPhotoAlternate: ImageVector? = null
private var _photoCamera: ImageVector? = null
private var _checkBoxOutlineBlank: ImageVector? = null