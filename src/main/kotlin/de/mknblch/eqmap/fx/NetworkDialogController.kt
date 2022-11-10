package de.mknblch.eqmap.fx

import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.config.FxmlResource
import de.mknblch.eqmap.config.IRCNetworkConfig
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import kotlin.random.Random

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
    private lateinit var portTextField: TextField

    @FXML
    private lateinit var secureCheckBox: CheckBox

    @FXML
    private lateinit var channelTextField: TextField

    @FXML
    private lateinit var nickTextField: TextField

    @FXML
    private lateinit var serverPasswordField: TextField

    @FXML
    private lateinit var channelPasswordTextField: TextField

    @FXML
    private lateinit var encryptionTextField: TextField

    private var xOffset = 0.0
    private var yOffset = 0.0

    private var configuration: IRCNetworkConfig? = null

    fun show(parent: Window): IRCNetworkConfig? {

        val stage = createStage(parent)
        //
        registerDragListener(stage)

        initTextFields()

        dialogPane.lookupButton(ButtonType.CANCEL).setOnMouseClicked {
            onCancelButtonClick(stage)
        }

        dialogPane.lookupButton(ButtonType.OK).setOnMouseClicked {
            onOKButtonClick(stage)
        }

        //
        stage.showAndWait()

        return configuration
    }

    private fun onCancelButtonClick(stage: Stage) {
        configuration = null
        stage.close()
    }

    private fun initTextFields() {
        serverTextField.textProperty().set(properties.getOrSet("ircServer", "irc.quakenet.org"))
        channelTextField.textProperty().set(properties.getOrSet("ircChannel", "#everquest"))
        nickTextField.textProperty().set(properties.getOrSet("ircNickname", randomName()))
        serverPasswordField.textProperty().set(properties.get("ircServerPassword"))
        channelPasswordTextField.textProperty().set(properties.get("ircChannelPassword"))
        encryptionTextField.textProperty().set(properties.get("ircEncryptionPassword"))
        secureCheckBox.selectedProperty().set(properties.getOrSet("ircSecure", true))
        portTextField.textProperty().set(properties.getOrSet("ircServerPort", 6665).toString())
    }

    private fun onOKButtonClick(stage: Stage) {
        configuration = IRCNetworkConfig(
            host = serverTextField.textProperty().get(),
            chan = channelTextField.textProperty().get(),
            nickName = nickTextField.textProperty().get(),
            chanPassword = serverPasswordField.textProperty().get(),
            serverPassword = channelPasswordTextField.textProperty().get(),
            port = portTextField.text.toInt(),
            secure = secureCheckBox.selectedProperty().get()
        )
        properties.set("ircServer", serverTextField.textProperty().get())
        properties.set("ircServerPort", portTextField.textProperty().get().toInt())
        properties.set("ircChannel", channelTextField.textProperty().get())
        properties.set("ircNickname", nickTextField.textProperty().get())
        properties.set("ircServerPassword", serverPasswordField.textProperty().get())
        properties.set("ircChannelPassword", channelPasswordTextField.textProperty().get())
        properties.set("ircSecure", secureCheckBox.selectedProperty().get())

        stage.close()
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

        private val head: List<Char> = ('A'..'Z').toList()
        private val tail: List<Char> = ('a'..'z') + ('0'..'9')

        private fun randomName() = head.random() +
                (0 until Random.nextInt(6, 8))
                    .map { tail.random() }
                    .joinToString("")

        private val logger = LoggerFactory.getLogger(NetworkDialogController::class.java)
    }
}