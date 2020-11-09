package com.squins.gdx.backends.bytecoder.preloader

import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.squins.gdx.backends.bytecoder.BytecoderFileHandle
import com.squins.gdx.backends.bytecoder.api.web.HtmlAudioElement
import com.squins.gdx.backends.bytecoder.api.web.HtmlImageElement
import com.squins.gdx.backends.bytecoder.makeAndLogIllegalArgumentException
import com.squins.gdx.backends.bytecoder.preloader.AssetDownloader.AssetLoaderListener
import com.squins.gdx.backends.bytecoder.preloader.AssetFilter.AssetType
import org.omg.CORBA.Object
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter


class Preloader(val baseUrl:String) {
    private val loader: AssetDownloader = AssetDownloader()

    interface PreloaderCallback {
        fun update(state: PreloaderState)
        fun error(file: String)
    }

    private var directories: ObjectMap<String, Void> = ObjectMap()
    val images: ObjectMap<String, HtmlImageElement> = ObjectMap()
    var audio: ObjectMap<String, HtmlAudioElement> = ObjectMap()
    var texts: ObjectMap<String, String> = ObjectMap()
    var binaries: ObjectMap<String, Blob> = ObjectMap()
    private val stillToFetchAssets: ObjectMap<String, Asset> = ObjectMap()
    var assetNames: ObjectMap<String, String> = ObjectMap()

    class Asset(val file: String,
                val url: String,
                val type: AssetType,
                val size: Long,
                val mimeType: String) {
        var succeed = false
        var failed = false
        var downloadStarted = false
        var loaded: Long = 0

    }

    class PreloaderState(val assets: List<Asset>) {
        private val downloadedSize: Long
            get() {
                var size: Long = 0
                for (element in assets) {
                    size += if (element.succeed || element.failed) element.size else element.size.coerceAtMost(element.loaded)
                }
                return size
            }

        private val totalSize: Long
            get() {
                var size: Long = 0
                for (element in assets) {
                    size += element.size
                }
                return size
            }

        val progress: Float
            get() {
                val total = totalSize
                return if (total == 0L) 1F else downloadedSize / total.toFloat()
            }

        fun hasEnded(): Boolean {
            return downloadedSize == totalSize
        }

    }



    fun preload(assetFileUrl: String, callback: PreloaderCallback) {
        println("preload called")

        println("assetFileUr: $assetFileUrl")
        loader.loadText(assetFileUrl + "?etag=" + System.currentTimeMillis(), object : AssetLoaderListener<String> {
            override fun onProgress(amount: Double) {}
            override fun onFailure() {
                callback.error(assetFileUrl)
            }

            override fun onSuccess(result: String) {
                println("onSucces, result: $result")
                val lines = result.split("\n".toRegex()).toTypedArray()
                println("after lines: ${lines.size}" )
                val assets = mutableListOf<Asset>()
                println("after assets: ${assets.size}" )
                for (line in lines) {
                    println("line in lines: $line")
                    val tokens = line.split(":".toRegex()).toTypedArray()
                    println(tokens.size)
                    for(token in tokens.withIndex()){
                        println(token.value)
                    }
                    if (tokens.size != 6) {
                        println("size not 6")
                        throw makeAndLogIllegalArgumentException("Preloader","Invalid assets description file.")
                    }
                    println("after check size")
                    val assetTypeCode = tokens[0]
                    println("after assetTypeCode: $assetTypeCode")

                    val assetPathOrig = tokens[1]
                    println("after assetPathOrig: $assetPathOrig")

                    val assetPathMd5 = tokens[2]
                    println("after assetPathMd5: $assetPathMd5")

                    var size = tokens[3].toLong()
                    println("after size: $size")

                    val assetMimeType = tokens[4]
                    println("after assetMimeType: $assetMimeType")

                    val assetPreload = tokens[5] == "1"
                    println("after assetPreload: $assetPreload")
                    var type: AssetType = AssetType.Text
                    if (assetTypeCode == "i") type = AssetType.Image
                    if (assetTypeCode == "b") type = AssetType.Binary
                    if (assetTypeCode == "a") type = AssetType.Audio
                    if (assetTypeCode == "d") type = AssetType.Directory
                    println("after type checking, type is: ${type.code}")
                    if (type === AssetType.Audio && !loader.isUseBrowserCache) {
                        println("audio and not isUseBrowserCache")
                        size = 0
                    }
                    val asset = Asset(assetPathOrig.trim(), assetPathMd5.trim(), type, size, assetMimeType)
                    println("after new asset, asset.file: ${asset.file}, asset.url: ${asset.url}")
                    val assetFile = asset.file
                    val assetUrl = asset.url

//                    val asset2 : ObjectMap<String, String> = ObjectMap()
//
//                    asset2.put(assetFile, assetUrl)
//
//                    for(entry in asset2.entries()){
//                        println(entry.key + entry.value)
//                    }
//
//                    println("after asset2.puttttttttttttt")
//
//                    println("assetFile: $assetFile, assetUrl: $assetUrl")
//
                    assetNames.put(assetFile, assetUrl)

                    for(entry in assetNames.entries()){
                        println(entry.key + entry.value)
                    }

                    println("after assetNames.put")
                    if (assetPreload || asset.file.startsWith("com/badlogic/")) {
                        println("before assets.add")
                        assets.add(asset)
                        println("after assets.add")
                    }
                    else {
                        println("add to stillToFetchAssets")
                        stillToFetchAssets.put(asset.file, asset);
                    }
                }
                println("before state = PreloaderState(assets)")
                val state = PreloaderState(assets)
                println("after state = PreloaderState(assets)")
                for (i in assets.withIndex()){
                    val asset : Asset = assets[i.index]

                    if(contains(asset.file)) {
                        println("true")
                        asset.loaded = asset.size
                        asset.succeed = true
                        continue
                    }

                    asset.downloadStarted = true
                    println("before loader.load")
                    loader.load(baseUrl + asset.url, asset.type, asset.mimeType, object: AssetLoaderListener<Any>{
                        override fun onProgress(amount: Double) {
                            println("onProgress")
                            asset.loaded = amount.toLong()
                            callback.update(state)
                        }

                        override fun onFailure() {
                            println("onFailure")
                            asset.failed = true
                            callback.error(asset.file)
                            callback.update(state)
                        }

                        override fun onSuccess(result: Any) {
                            println("onSuccess")
                            putAssetInMap(result, asset)
                            asset.succeed = true
                            callback.update(state)
                        }

                    })
                }
                callback.update(state)
            }
        })
    }

// TODO coen: this method is now directly called, change to load via assets.txt
    fun doLoadAssets(assets: List<Asset>, callback: PreloaderCallback) {
        println("doLoadAssets called, assets.size: ${assets.size}")
        val state = PreloaderState(assets)
        println("created state")
        for (element in assets) {
            println("loop, asset: ${element.file}")
            if (contains(element.file)) {
                element.loaded = element.size
                element.succeed = true
                continue
            }
            element.downloadStarted = true
            loader.load(baseUrl + "/" + element.url, element.type, element.mimeType, object : AssetLoaderListener<Any?> {
                override fun onProgress(amount: Double) {
                    println("onProgress")
                    element.loaded = amount.toLong()
                    callback.update(state)
                }

                override fun onFailure() {
                    println("onFailure")
                    element.failed = true
                    callback.error(element.file)
                    callback.update(state)
                }

                override fun onSuccess(result: Any?) {
                    println("onSuccess")
                    putAssetInMap(result, element)
                    element.succeed = true
                    callback.update(state)
                }
            })
        }
        callback.update(state)
    }

    fun preloadSingleFile(file: String?) {
        if (!isNotFetchedYet(file)) return
        val asset: Asset = stillToFetchAssets.get(file)
        if (asset.downloadStarted) return
        Gdx.app.log("Preloader", "Downloading " + baseUrl + asset.file)
        asset.downloadStarted = true
        loader.load(baseUrl + asset.url, asset.type, asset.mimeType, object : AssetLoaderListener<Any?> {
            override fun onProgress(amount: Double) {
                asset.loaded = amount.toLong()
            }

            override fun onFailure() {
                asset.failed = true
                stillToFetchAssets.remove(file)
            }

            override fun onSuccess(result: Any?) {
                putAssetInMap(result, asset)
                stillToFetchAssets.remove(file)
                asset.succeed = true
            }
        })
    }

    protected fun putAssetInMap(result: Any?, asset: Asset) {
        println("putAssetInMap called(X)")
        println("putAssetInMap asset.file: ${asset.file}")
        println("result is null: before")
        println("result is null: ${result == null}")
        println("asset.type: ${asset.type}")
        println("asset.type.code: ${asset.type.code}")

        when (asset.type.code) {
            AssetType.Text.code -> texts.put(asset.file, result as String?)
            AssetType.Image.code -> images.put(asset.file, result as HtmlImageElement?)
            AssetType.Binary.code -> binaries.put(asset.file, result as Blob?)
            AssetType.Audio.code -> audio.put(asset.file, result as HtmlAudioElement?)
            AssetType.Directory.code -> directories.put(asset.file, null)
        }
        println("After putImageInMap when, sizes: texts: ${texts.size}, images: ${images.size}, audio: ${audio.size} ")
    }

//    fun read(file: String?): InputStream? {
//        if (texts.containsKey(file)) {
//            return try {
//                ByteArrayInputStream(texts.get(file).getBytes("UTF-8"))
//            } catch (e: UnsupportedEncodingException) {
//                null
//            }
//        }
//        if (images.containsKey(file)) {
//            return ByteArrayInputStream(ByteArray(1)) // FIXME, sensible?
//        }
//        if (binaries.containsKey(file)) {
//            return binaries.get(file).read()
//        }
//        return if (audio.containsKey(file)) {
//            audio.get(file).read()
//        } else null
//    }

    operator fun contains(file: String?): Boolean {
        return texts.containsKey(file) || images.containsKey(file) || binaries.containsKey(file) || audio.containsKey(file) || directories.containsKey(file)
    }

    private fun isNotFetchedYet(file: String?): Boolean {
        return stillToFetchAssets.containsKey(file)
    }

    fun isText(file: String?): Boolean {
        return texts.containsKey(file)
    }

    fun isImage(file: String?): Boolean {
        return images.containsKey(file)
    }

    fun isBinary(file: String?): Boolean {
        return binaries.containsKey(file)
    }

    fun isAudio(file: String?): Boolean {
        return audio.containsKey(file)
    }

    fun isDirectory(file: String?): Boolean {
        return directories.containsKey(file)
    }

    private fun isChild(filePath: String, directory: String): Boolean {
        return filePath.startsWith("$directory/") && filePath.indexOf('/', directory.length + 1) < 0
    }

    fun list(file: String): Array<FileHandle> {
        return getMatchedAssetFiles(object : FilePathFilter {
            override fun accept(path: String): Boolean {
                return isChild(path, file)
            }
        })
    }

    fun list(file: String, filter: FileFilter): Array<FileHandle> {
        return getMatchedAssetFiles(object : FilePathFilter {
            override fun accept(path: String): Boolean {
                return isChild(path, file) && filter.accept(File(path))
            }
        })
    }

    fun list(file: String, filter: FilenameFilter): Array<FileHandle> {
        return getMatchedAssetFiles(object : FilePathFilter {
            override fun accept(path: String): Boolean {
                return isChild(path, file) && filter.accept(File(file), path.substring(file.length + 1))
            }
        })
    }

    fun list(file: String, suffix: String?): Array<FileHandle> {
        return getMatchedAssetFiles(object : FilePathFilter {
            override fun accept(path: String): Boolean {
                return isChild(path, file) && path.endsWith(suffix!!)
            }
        })
    }

//    fun length(file: String): Long {
//        if (texts.containsKey(file)) {
//            return try {
//                texts.get(file).getBytes("UTF-8").length
//            } catch (e: UnsupportedEncodingException) {
//                texts.get(file).getBytes().length
//            }
//        }
//        if (images.containsKey(file)) {
//            return 1 // FIXME, sensible?
//        }
//        if (binaries.containsKey(file)) {
//            return binaries.get(file).length().toLong()
//        }
//        return if (audio.containsKey(file)) {
//            audio.get(file).length()
//        } else 0
//    }

    private interface FilePathFilter {
        fun accept(path: String): Boolean
    }

    private fun getMatchedAssetFiles(filter: FilePathFilter): Array<FileHandle> {
        // TODO: implement ok?
        val files: Array<FileHandle> = arrayOf()
        for (file in assetNames.keys()) {
            if (filter.accept(file)) {
                //fix set index needed
                mutableListOf<FileHandle>(BytecoderFileHandle(this, file, Files.FileType.Internal))
            }
        }
        val filesArray: Array<FileHandle> = arrayOf()
        System.arraycopy(files, 0, filesArray, 0, filesArray.size)
        return filesArray
//        return arrayOf()
    }

}