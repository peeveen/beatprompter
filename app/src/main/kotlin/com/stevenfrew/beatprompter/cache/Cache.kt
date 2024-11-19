package com.stevenfrew.beatprompter.cache

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.BuildConfig
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.AudioFileParser
import com.stevenfrew.beatprompter.cache.parse.ImageFileParser
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cache.parse.MidiAliasFileParser
import com.stevenfrew.beatprompter.cache.parse.SetListFileParser
import com.stevenfrew.beatprompter.cache.parse.SongInfoParser
import com.stevenfrew.beatprompter.cache.parse.SupportFileResolver
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.storage.CacheFolder
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.DownloadTask
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
import com.stevenfrew.beatprompter.util.getMd5Hash
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object Cache {
	object CacheEventHandler : Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.CLEAR_CACHE -> clearCache(msg.obj as Boolean)
				Events.CACHE_UPDATED -> onCacheUpdated(msg.obj as CachedCloudCollection)
				Events.CLOUD_SYNC_ERROR -> EventRouter.sendEventToSongList(msg.what, msg.obj)
			}
		}
	}

	internal val supportFileResolver: SupportFileResolver = object : SupportFileResolver {
		override fun getMappedAudioFiles(filename: String): List<AudioFile> =
			cachedCloudItems.getMappedAudioFiles(filename)

		override fun getMappedImageFiles(filename: String): List<ImageFile> =
			cachedCloudItems.getMappedImageFiles(filename)
	}

	private const val XML_DATABASE_FILE_NAME = "bpdb.xml"
	private const val TEMP_XML_DATABASE_FILE_NAME = "$XML_DATABASE_FILE_NAME.tmp"
	private const val XML_DATABASE_FILE_ROOT_ELEMENT_TAG = "beatprompterDatabase"
	private const val TEMPORARY_SET_LIST_FILENAME = "temporary_setlist.txt"
	private const val DEFAULT_MIDI_ALIASES_FILENAME = "default_midi_aliases.txt"
	private const val XML_DATABASE_VERSION_ATTRIBUTE = "version"

	var defaultDownloads: MutableList<DownloadResult> = mutableListOf()
	var cachedCloudItems = CachedCloudCollection()

	private var beatPrompterDataFolder: File? = null
	private var beatPrompterSongFilesFolder: File? = null

	// Fake storage items for temporary set list and default midi aliases
	var temporarySetListFile: File? = null
	var defaultMidiAliasesFile: File? = null

	fun copyAssetsFileToLocalFolder(filename: String, destination: File) =
		BeatPrompter.appResources.assetManager.open(filename).use { inStream ->
			val outputStream = FileOutputStream(destination)
			outputStream.use {
				Utils.streamToStream(inStream, it)
			}
		}

	fun initialiseTemporarySetListFile(deleteExisting: Boolean, context: Context) {
		try {
			if (deleteExisting)
				if (!temporarySetListFile!!.delete())
					Logger.log("Could not delete temporary set list file.")
			if (!temporarySetListFile!!.exists())
				Utils.appendToTextFile(
					temporarySetListFile!!,
					String.format("{set:%1\$s}", context.getString(R.string.temporary))
				)
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}
	}

	fun initialiseLocalStorage(context: Context) {
		val previousSongFilesFolder = beatPrompterSongFilesFolder
		val s = context.packageName
		try {
			val m = context.packageManager
			val p = m.getPackageInfo(s, 0)
			beatPrompterDataFolder = File(p.applicationInfo.dataDir)
		} catch (e: PackageManager.NameNotFoundException) {
			// There is no way that this can happen.
			Logger.log("Package name not found ", e)
		}

		val songFilesFolder: String
		val useExternalStorage = BeatPrompter.preferences.useExternalStorage
		val externalFilesDir = context.getExternalFilesDir(null)
		songFilesFolder = if (useExternalStorage && externalFilesDir != null)
			externalFilesDir.absolutePath
		else
			beatPrompterDataFolder!!.absolutePath

		beatPrompterSongFilesFolder =
			if (songFilesFolder.isEmpty()) beatPrompterDataFolder else File(songFilesFolder)
		if (!beatPrompterSongFilesFolder!!.exists())
			if (!beatPrompterSongFilesFolder!!.mkdir())
				Logger.log("Failed to create song files folder.")

		if (!beatPrompterSongFilesFolder!!.exists())
			beatPrompterSongFilesFolder = beatPrompterDataFolder

		temporarySetListFile = File(beatPrompterDataFolder, TEMPORARY_SET_LIST_FILENAME)
		defaultMidiAliasesFile = File(beatPrompterDataFolder, DEFAULT_MIDI_ALIASES_FILENAME)
		val temporarySetListFileMd5Checksum = temporarySetListFile!!.getMd5Hash()
		val defaultMidiAliasesFileMd5Checksum = defaultMidiAliasesFile!!.getMd5Hash()
		initialiseTemporarySetListFile(false, context)
		try {
			copyAssetsFileToLocalFolder(DEFAULT_MIDI_ALIASES_FILENAME, defaultMidiAliasesFile!!)
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}

		defaultDownloads.apply {
			clear()
			add(
				SuccessfulDownloadResult(
					FileInfo(
						"idBeatPrompterTemporarySetList",
						"BeatPrompterTemporarySetList",
						Date(),
						temporarySetListFileMd5Checksum
					), temporarySetListFile!!
				)
			)
			add(
				SuccessfulDownloadResult(
					FileInfo(
						"idBeatPrompterDefaultMidiAliases",
						context.getString(R.string.default_alias_set_name),
						Date(),
						defaultMidiAliasesFileMd5Checksum
					), defaultMidiAliasesFile!!
				)
			)
		}

		if (previousSongFilesFolder != null)
			if (previousSongFilesFolder != beatPrompterSongFilesFolder)
			// Song file storage folder has changed. We need to clear the cache.
				clearCache(false)
	}

	private fun <TCachedCloudItemType : CachedItem> addToCollection(
		xmlDoc: Document,
		tagName: String,
		parser: (cachedItem: Element, useXmlData: Boolean) -> TCachedCloudItemType,
		itemSource: PublishSubject<CachedItem>,
		messageSource: PublishSubject<Pair<String?, Boolean>>,
		rebuild: Boolean
	) {
		val elements = xmlDoc.getElementsByTagName(tagName)
		repeat(elements.length) {
			val element = elements.item(it) as Element
			try {
				val cachedItem = parser(element, !rebuild)
				itemSource.onNext(cachedItem)
				messageSource.onNext(Pair(cachedItem.name, false))
			} catch (exception: InvalidBeatPrompterFileException) {
				messageSource.onNext(
					Pair(
						exception.message
							?: BeatPrompter.appResources.getString(R.string.failedToReadDatabaseItem), false
					)
				)
				itemSource.onError(exception)
				// This should never happen. If we could write out the file info, then it was valid.
				// So it must still be valid when we come to read it in. Unless some dastardly devious sort
				// has meddled with files outside of the app ...
				Logger.log("Failed to parse file.")
				// File has become irrelevant
				itemSource.onNext(IrrelevantFile(CachedFile(element)))
			}
		}
	}

	private fun readFromXML(
		xmlDoc: Document,
		itemSource: PublishSubject<CachedItem>,
		messageSource: PublishSubject<Pair<String?, Boolean>>
	): Boolean {
		val xmlDatabaseVersion = xmlDoc.documentElement.getAttribute(XML_DATABASE_VERSION_ATTRIBUTE)
		val currentAppVersion = "${BuildConfig.VERSION_CODE}"
		val rebuild = BuildConfig.DEBUG || currentAppVersion != xmlDatabaseVersion
		messageSource.onNext(Pair(null, rebuild))
		cachedCloudItems.clear()
		PARSINGS.forEach {
			addToCollection(
				xmlDoc,
				it.first.findAnnotation<CacheXmlTag>()!!.tag,
				it.second,
				itemSource,
				messageSource,
				rebuild
			)
		}
		return rebuild
	}

	fun readDatabase(listener: CacheReadListener): Boolean {
		val database = File(beatPrompterDataFolder, XML_DATABASE_FILE_NAME)
		val databaseExists = database.exists()
		if (databaseExists) {
			val itemSource = PublishSubject.create<CachedItem>()
			val messageSource = PublishSubject.create<Pair<String?, Boolean>>()
			val compositeDisposable = CompositeDisposable().apply {
				add(
					itemSource.subscribe(
						{ listener.onItemRead(it) },
						{ listener.onCacheReadError(it) },
						{ listener.onCacheReadComplete() })
				)
				add(messageSource.subscribe {
					Utils.reportProgress(listener, it)
				})
			}

			BeatPrompter.addDebugMessage("Reading XML database.")
			try {
				val xml = DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.parse(database)
				if (readFromXML(
						xml,
						itemSource,
						messageSource
					)
				)
					writeDatabase()
				BeatPrompter.addDebugMessage("Calling itemSource.onComplete()")
				itemSource.onComplete()
				BeatPrompter.addDebugMessage("Called itemSource.onComplete()")
				compositeDisposable.dispose()
				BeatPrompter.addDebugMessage("Disposed of CompositeDisposable.")
			} catch (e: Exception) {
				// Something went wrong with reading the XML.
				// Database is lost, and will need rebuilt.
				return false
			}
		}
		return databaseExists
	}

	fun writeDatabase() {
		try {
			val tempDatabase = File(beatPrompterDataFolder, TEMP_XML_DATABASE_FILE_NAME)
			val database = File(beatPrompterDataFolder, XML_DATABASE_FILE_NAME)
			tempDatabase.delete()
			val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			val d = docBuilder.newDocument()
			val root = d.createElement(XML_DATABASE_FILE_ROOT_ELEMENT_TAG)
			root.setAttribute(
				XML_DATABASE_VERSION_ATTRIBUTE,
				"${BuildConfig.VERSION_CODE}"
			)
			d.appendChild(root)
			cachedCloudItems.writeToXML(d, root)
			val transformer = TransformerFactory.newInstance().newTransformer()
			val output = StreamResult(tempDatabase)
			val input = DOMSource(d)
			transformer.transform(input, output)
			// Rename temp database file to actual database.
			// This limits potential for corruption caused by shutdown during write.
			tempDatabase.copyTo(database, true)
			tempDatabase.delete()
		} catch (ioe: Exception) {
			Logger.log(ioe)
			EventRouter.sendEventToSongList(Events.DATABASE_WRITE_ERROR)
		}
	}

	fun getCacheFolderForStorage(storage: StorageType): CacheFolder =
		CacheFolder(beatPrompterSongFilesFolder!!, Storage.getCacheFolderName(storage))

	fun clearCache(report: Boolean) {
		// Clear both cache folders
		val cacheFolder = getCacheFolderForStorage(BeatPrompter.preferences.storageSystem)
		cacheFolder.clear()
		cachedCloudItems.clear()
		writeDatabase()
		EventRouter.sendEventToSongList(Events.CACHE_CLEARED, report)
	}

	fun onCacheUpdated(cache: CachedCloudCollection) {
		cachedCloudItems = cache
		EventRouter.sendEventToSongList(Events.CACHE_UPDATED, cache)
	}

	private val cloudPath: String
		get() = BeatPrompter.preferences.cloudPath

	private val includeSubFolders: Boolean
		get() = BeatPrompter.preferences.includeSubFolders

	fun canPerformCloudSync(): Boolean =
		BeatPrompter.preferences.storageSystem !== StorageType.Demo && cloudPath.isNotBlank()

	fun performFullCloudSync(parentFragment: Fragment): Boolean =
		performCloudSync(null, false, parentFragment)

	fun clearTemporarySetList(context: Context) {
		for (slf in cachedCloudItems.setListFiles)
			if (slf.file == temporarySetListFile)
				slf.setListEntries.clear()
		initialiseTemporarySetListFile(true, context)
		writeDatabase()
		EventRouter.sendEventToSongList(Events.TEMPORARY_SET_LIST_CLEARED)
	}

	fun performCloudSync(
		fileToUpdate: CachedFile?,
		dependenciesToo: Boolean,
		parentFragment: Fragment
	): Boolean {
		val context = parentFragment.requireContext()
		if (fileToUpdate == null)
			clearTemporarySetList(context)
		val cs = Storage.getInstance(BeatPrompter.preferences.storageSystem, parentFragment)
		val cloudPath = cloudPath
		return if (cloudPath.isBlank()) {
			Toast.makeText(
				context,
				context.getString(R.string.no_cloud_folder_currently_set),
				Toast.LENGTH_LONG
			)
				.show()
			false
		} else {
			DownloadTask(
				parentFragment.requireContext(),
				cs,
				CacheEventHandler,
				cloudPath,
				includeSubFolders,
				cachedCloudItems.getFilesToRefresh(fileToUpdate, dependenciesToo)
			).execute(Unit)
			true
		}
	}

	private val PARSINGS =
		listOf<Pair<KClass<out CachedItem>, (item: Element, useXmlData: Boolean) -> CachedItem>>(
			CachedFolder::class to { element, _ -> CachedFolder(element) },
			AudioFile::class to { element, useXmlData ->
				AudioFileParser(CachedFile(element)).parse(
					if (useXmlData) element else null
				)
			},
			ImageFile::class to { element, useXmlData ->
				ImageFileParser(CachedFile(element)).parse(
					if (useXmlData) element else null
				)
			},
			SongFile::class to { element, useXmlData ->
				SongInfoParser(CachedFile(element)).parse(
					if (useXmlData) element else null
				)
			},
			SetListFile::class to { element, useXmlData ->
				SetListFileParser(CachedFile(element)).parse(
					if (useXmlData) element else null
				)
			},
			MIDIAliasFile::class to { element, useXmlData ->
				MidiAliasFileParser(CachedFile(element)).parse(
					if (useXmlData) element else null
				)
			},
			IrrelevantFile::class to { element, _ -> IrrelevantFile(CachedFile(element)) }
		)
}

