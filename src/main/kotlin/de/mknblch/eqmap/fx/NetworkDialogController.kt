package de.mknblch.eqmap.fx

import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.config.FxmlResource
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.ButtonType
import javafx.scene.control.DialogPane
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import kotlin.random.Random
import kotlin.random.nextULong


data class IRCNetworkConfig(
    val host: String,
    val chan: String,
    val nickName: String,
    val chanPassword: String?,
    val serverPassword: String?,
)

@Lazy
@Component
@FxmlResource("fxml/NetworkDialog.fxml")
class NetworkDialogController {

    @Autowired
    private lateinit var properties: PersistentProperties

    @FXML
    private lateinit var dialogPane: DialogPane

    @FXML
    private lateinit var serverTextField: TextField

    @FXML
    private lateinit var channelTextField: TextField

    @FXML
    private lateinit var nickTextField: TextField

    @FXML
    private lateinit var serverPasswordTextField: PasswordField

    @FXML
    private lateinit var channelPasswordTextField: PasswordField

    private var xOffset = 0.0
    private var yOffset = 0.0

    private var configuration: IRCNetworkConfig? = null

    fun show(parent: Window): IRCNetworkConfig? {

        val stage = createStage(parent)
        //
        registerDragListener(stage)

        initTextFields()

        dialogPane.lookupButton(ButtonType.CANCEL).setOnMouseClicked {
            configuration = null
            stage.close()
        }

        dialogPane.lookupButton(ButtonType.OK).setOnMouseClicked {
            configuration = IRCNetworkConfig(
                host = serverTextField.textProperty().get(),
                chan = channelTextField.textProperty().get(),
                nickName = nickTextField.textProperty().get(),
                chanPassword = serverPasswordTextField.textProperty().get(),
                serverPassword = channelPasswordTextField.textProperty().get(),
            )
            properties.set("ircServer", serverTextField.textProperty().get())
            properties.set("ircChannel", channelTextField.textProperty().get())
            properties.set("ircNickname", nickTextField.textProperty().get())
            properties.set("ircServerPassword", serverPasswordTextField.textProperty().get())
            properties.set("ircChannelPassword", channelPasswordTextField.textProperty().get())

            stage.close()
        }

        //
        stage.showAndWait()

        return configuration
    }

    private fun initTextFields() {
        serverTextField.textProperty().set(properties.getOrSet("ircServer", "irc.quakenet.org"))
        channelTextField.textProperty().set(properties.getOrSet("ircChannel", "#everquest"))
        nickTextField.textProperty().set(properties.getOrSet("ircNickname", Random.nextULong().toString(16)))
        serverPasswordTextField.textProperty().set(properties.get("ircServerPassword"))
        channelPasswordTextField.textProperty().set(properties.get("ircChannelPassword"))
    }

    private fun createStage(parent: Window): Stage {
        val stage = Stage(StageStyle.UNDECORATED)
        stage.initOwner(parent)
        stage.initModality(Modality.APPLICATION_MODAL);

        val scene = Scene(dialogPane)
        scene.stylesheets.add(javaClass.classLoader.getResource("style.css")!!.toExternalForm())
        stage.scene = scene
        return stage
    }

    private fun registerDragListener(stage: Stage) {
        dialogPane.onMousePressed = EventHandler { event ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }

        dialogPane.onMouseDragged = EventHandler { event ->
            stage.x = event.screenX - xOffset
            stage.y = event.screenY - yOffset
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NetworkDialogController::class.java)
    }
}