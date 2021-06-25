package main

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mygdx.game.MyGdxGame
import com.mygdx.game.MyGdxGame2
import com.squins.gdx.backends.bytecoder.BytecoderApplication
import com.squins.gdx.backends.bytecoder.api.web.ExtWindow
import com.squins.gdx.backends.bytecoder.api.web.HTMLDivElement
import com.squins.gdx.backends.bytecoder.api.web.LibgdxAppCanvas
import com.squins.gdx.backends.bytecoder.graphics.BytecoderGL20
import com.squins.gdx.backends.bytecoder.preloader.Preloader
import com.squins.gdx.backends.bytecoder.preloader.PreloaderBundleGenerator
import com.squins.gdx.backends.bytecoder.preloader.PreloaderCallback
import com.squins.gdx.backends.bytecoder.preloader.PreloaderState
import de.mirkosertic.bytecoder.api.web.HTMLDocument
import de.mirkosertic.bytecoder.api.web.Window
import main.examples.webgl.LibGDXBytecoderGL20SampleWebGlShaders

class Main {

    private val window: ExtWindow
    private val document: HTMLDocument
    val scale: Float
    private val app: HTMLDivElement
    private val libgdxAppCanvas: LibgdxAppCanvas

    // TODO: move this to external class, only used when running runSimpleGlExampleNoLibgdx

    init {
        println("assign window")
        window = Window.window()!! as ExtWindow
        println("assign document")
        document = window.document()
        println("assign scale")
        scale = window.devicePixelRatio
        println("assign app")
        app = (document.getElementById("app") as HTMLDivElement)
        println("assign libgdxAppCanvas")
        libgdxAppCanvas = document.querySelector("#canvas1") as LibgdxAppCanvas
        println("app.style")
        app.style("float:left; width:100%; height:100%;")
    }

    private fun thingsToKeep() {
        val preloaderBundleGenerator:PreloaderBundleGenerator? = null
    }

    private fun runLibGdxExample() {
        println("runLibGdxExample")
        BytecoderApplication(MyGdxGame(), libgdxAppCanvas)
    }



    private fun justPreload() {
        println("justPreload called")
        val baseUrl = libgdxAppCanvas.assetBaseUrl()
        println("assetBaseUrl: $baseUrl")

        println("creating preloader")
        val preloader = Preloader(baseUrl)

        val assetFileUrl = "$baseUrl/assets.txt"

        println("calling preloader.preload()")
        preloader.preload(assetFileUrl, object : PreloaderCallback {
            override fun update(state: PreloaderState) {
                println("update not implemented")
            }

            override fun error(file: String) {
                println("error not implemented")
            }

        })
    }



    private fun runSimpleGlExampleSimpleApp(){
        println("runSimpleGlExampleNoLibgdx")
        val gl = libgdxAppCanvas.getContext("webgl")
//        BytecoderSampleWebGlShaders(app, libgdxAppCanvas, gl).run()
//        BytecoderSampleAudio(libgdxAppCanvas).run()
        LibGDXBytecoderGL20SampleWebGlShaders(app, libgdxAppCanvas, BytecoderGL20(gl)).run()
    }

    private fun runGdxScreenExample() {
        println("runGdxScreenExample")
        BytecoderApplication(MyGdxGame2(), libgdxAppCanvas)
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>?) {
            println("Start in 3 2 1 go")

            val clazz: Any = Int::class.java

            println("This line will crash too")
            println("Clazz hashcode " + clazz.hashCode())

            println("This line will crash:")
            println("Clazz instanceof: " + (clazz !is String))

            println("Creating region")
            var textureRegion:TextureRegion = TextureRegion()

//            val clazz:Any = 5.0


//            println("instanceof string? " + (clazz is String))

            println("creating resources map")
            val resources: ObjectMap<Class<*>?, ObjectMap<String, Any>?> = ObjectMap()
            println("resources.get()")
            resources.get(TextureRegion::class.java)
            println("Get done, go to put")
            resources.put(TextureRegion::class.java, ObjectMap())
            println("put done")


            // TODO: make it configurable which example to run. Dropdown choice in de HTML document?
            Main().runGdxScreenExample()
//            Main().runLibGdxExample()
        }
    }
}

