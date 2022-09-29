package de.mknblch.eqmap.config

import org.mockito.Mock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootApplication
class TestAppStarter {

    @MockBean
    private lateinit var directoryWatcherConfig: DirectoryWatcherConfig

    @MockBean
    private lateinit var springFXMLLoader: SpringFXMLLoader

}