package de.mknblch.eqmap

import kotlin.jvm.JvmOverloads
import javafx.stage.Stage
import javafx.beans.property.BooleanProperty
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.input.MouseEvent

/**
 * Util class to handle window resizing when a stage style set to StageStyle.UNDECORATED.
 * Includes dragging of the stage.
 * Original on 6/13/14.
 * Updated on 8/15/17.
 * Updated on 12/19/19.
 *
 * @author Alexander.Berg
 * @author Evgenii Kanivets
 * @author Zachary Perales
 */
object ResizeHelper {
    @JvmOverloads
    fun addResizeListener(
        resizeAllowed: BooleanProperty, stage: Stage, border: Int = 3
    ) {
        val resizeListener = ResizeListener(resizeAllowed, stage, border)
        stage.scene.addEventHandler(MouseEvent.MOUSE_MOVED, resizeListener)
        stage.scene.addEventHandler(MouseEvent.MOUSE_PRESSED, resizeListener)
        stage.scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, resizeListener)
        stage.scene.addEventHandler(MouseEvent.MOUSE_EXITED, resizeListener)
        stage.scene.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, resizeListener)
        resizeListener.minWidth = 1.0
        resizeListener.minHeight = 1.0
        resizeListener.maxWidth = Double.MAX_VALUE
        resizeListener.maxHeight = Double.MAX_VALUE

        stage.scene.root.childrenUnmodifiable.forEach {
            addListenerDeeply(it, resizeListener, 1)
        }
    }

    private fun addListenerDeeply(node: Node, listener: EventHandler<MouseEvent>, depth: Int = Integer.MAX_VALUE) {
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, listener)
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener)
        node.addEventHandler(MouseEvent.MOUSE_EXITED, listener)
        node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener)
        node.addEventHandler(MouseEvent.MOUSE_MOVED, listener)
        if (depth <= 0) {
            return
        }

        if (node is Parent) {
            val children = node.childrenUnmodifiable
            for (child in children) {
                addListenerDeeply(child, listener, depth - 1)
            }
        }
    }

    internal class ResizeListener(private val lockWindow: BooleanProperty, private val stage: Stage, private val border: Int) : EventHandler<MouseEvent> {
        private var cursorEvent = Cursor.DEFAULT
        private var resizing = true
        private var startX = 0.0
        private var startY = 0.0
        private var screenOffsetX = 0.0
        private var screenOffsetY = 0.0

        // Max and min sizes for controlled stage
        var minWidth = 0.0
        var maxWidth = 0.0
        var minHeight = 0.0
        var maxHeight = 0.0


        override fun handle(mouseEvent: MouseEvent) {
            if (lockWindow.get()) {
                return
            }
            val sceneWidth = stage.scene.width
            val sceneHeight = stage.scene.height
            if (MouseEvent.MOUSE_MOVED == mouseEvent.eventType) {
                cursorEvent = if (mouseEvent.sceneX < border && mouseEvent.sceneY < border) {
                    Cursor.NW_RESIZE
                } else if (mouseEvent.sceneX < border && mouseEvent.sceneY > sceneHeight - border) {
                    Cursor.SW_RESIZE
                } else if (mouseEvent.sceneX > sceneWidth - border && mouseEvent.sceneY < border) {
                    Cursor.NE_RESIZE
                } else if (mouseEvent.sceneX > sceneWidth - border && mouseEvent.sceneY > sceneHeight - border) {
                    Cursor.SE_RESIZE
                } else if (mouseEvent.sceneX < border) {
                    Cursor.W_RESIZE
                } else if (mouseEvent.sceneX > sceneWidth - border) {
                    Cursor.E_RESIZE
                } else if (mouseEvent.sceneY < border) {
                    Cursor.N_RESIZE
                } else if (mouseEvent.sceneY > sceneHeight - border) {
                    Cursor.S_RESIZE
                } else {
                    Cursor.DEFAULT
                }
                stage.scene.cursor = cursorEvent
            } else if (MouseEvent.MOUSE_EXITED == mouseEvent.eventType || MouseEvent.MOUSE_EXITED_TARGET == mouseEvent.eventType) {
                stage.scene.cursor = Cursor.DEFAULT
            } else if (MouseEvent.MOUSE_PRESSED == mouseEvent.eventType) {
                startX = stage.width - mouseEvent.sceneX
                startY = stage.height - mouseEvent.sceneY
            } else if (MouseEvent.MOUSE_DRAGGED == mouseEvent.eventType) {
                if (Cursor.DEFAULT != cursorEvent) {
                    resizing = true
                    if (Cursor.W_RESIZE != cursorEvent && Cursor.E_RESIZE != cursorEvent) {
                        val minHeight = if (stage.minHeight > border * 2) stage.minHeight else (border * 2).toDouble()
                        if (Cursor.NW_RESIZE == cursorEvent || Cursor.N_RESIZE == cursorEvent || Cursor.NE_RESIZE == cursorEvent) {
                            if (stage.height > minHeight || mouseEvent.sceneY < 0) {
                                setStageHeight(stage.y - mouseEvent.screenY + stage.height)
                                stage.y = mouseEvent.screenY
                            }
                        } else {
                            if (stage.height > minHeight || mouseEvent.sceneY + startY - stage.height > 0) {
                                setStageHeight(mouseEvent.sceneY + startY)
                            }
                        }
                    }
                    if (Cursor.N_RESIZE != cursorEvent && Cursor.S_RESIZE != cursorEvent) {
                        val minWidth = if (stage.minWidth > border * 2) stage.minWidth else (border * 2).toDouble()
                        if (Cursor.NW_RESIZE == cursorEvent || Cursor.W_RESIZE == cursorEvent || Cursor.SW_RESIZE == cursorEvent) {
                            if (stage.width > minWidth || mouseEvent.sceneX < 0) {
                                setStageWidth(stage.x - mouseEvent.screenX + stage.width)
                                stage.x = mouseEvent.screenX
                            }
                        } else {
                            if (stage.width > minWidth || mouseEvent.sceneX + startX - stage.width > 0) {
                                setStageWidth(mouseEvent.sceneX + startX)
                            }
                        }
                    }
                    resizing = false
                    mouseEvent.consume()
                }
            }
//            if (MouseEvent.MOUSE_PRESSED == mouseEvent.eventType && Cursor.DEFAULT == cursorEvent) {
//                resizing = false
//                screenOffsetX = stage.x - mouseEvent.screenX
//                screenOffsetY = stage.y - mouseEvent.screenY
//            }
//            if (MouseEvent.MOUSE_DRAGGED == mouseEvent.eventType && Cursor.DEFAULT == cursorEvent && !resizing) {
//                stage.x = mouseEvent.screenX + screenOffsetX
//                stage.y = mouseEvent.screenY + screenOffsetY
//            }
        }

        private fun setStageWidth(width: Double) {
            var width = width
            width = width.coerceAtMost(maxWidth)
            width = width.coerceAtLeast(minWidth)
            stage.width = width
        }

        private fun setStageHeight(height: Double) {
            var height = height
            height = height.coerceAtMost(maxHeight)
            height = height.coerceAtLeast(minHeight)
            stage.height = height
        }
    }
}