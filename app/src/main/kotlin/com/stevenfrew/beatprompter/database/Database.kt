package com.stevenfrew.beatprompter.database

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CacheXmlTag
import com.stevenfrew.beatprompter.cache.CachedCloudCollection
import com.stevenfrew.beatprompter.cache.CachedCloudCollection.Companion.PARSINGS
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.CachedItem
import com.stevenfrew.beatprompter.cache.IrrelevantFile
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.storage.CacheFolder
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.DownloadTask
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import com.stevenfrew.beatprompter.ui.SongListFragment.Companion.mSongListEventHandler
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
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
import kotlin.reflect.full.findAnnotation

object Database {
	// TODO: Figure out when to call dispose on this.
	private val mCompositeDisposable = CompositeDisposable()

	object DatabaseEventHandler : Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.CLEAR_CACHE -> clearCache(msg.obj as Boolean)
				Events.CACHE_UPDATED -> onCacheUpdated(msg.obj as CachedCloudCollection)
			}
		}
	}

	private const val XML_DATABASE_FILE_NAME = "bpdb.xml"
	private const val XML_DATABASE_FILE_ROOT_ELEMENT_TAG = "beatprompterDatabase"
	private const val TEMPORARY_SET_LIST_FILENAME = "temporary_setlist.txt"
	private const val DEFAULT_MIDI_ALIASES_FILENAME = "default_midi_aliases.txt"

	var mDefaultDownloads: MutableList<DownloadResult> = mutableListOf()
	var mCachedCloudItems = CachedCloudCollection()

	private var mBeatPrompterDataFolder: File? = null
	private var mBeatPrompterSongFilesFolder: File? = null

	// Fake storage items for temporary set list and default midi aliases
	var mTemporarySetListFile: File? = null
	var mDefaultMidiAliasesFile: File? = null

	fun copyAssetsFileToLocalFolder(filename: String, destination: File) {
		val inputStream = BeatPrompter.assetManager.open(filename)
		inputStream.use { inStream ->
			val outputStream = FileOutputStream(destination)
			outputStream.use {
				Utils.streamToStream(inStream, it)
			}
		}
	}

	fun initialiseTemporarySetListFile(deleteExisting: Boolean, context: Context) {
		try {
			if (deleteExisting)
				if (!mTemporarySetListFile!!.delete())
					Logger.log("Could not delete temporary set list file.")
			if (!mTemporarySetListFile!!.exists())
				Utils.appendToTextFile(
					mTemporarySetListFile!!,
					String.format("{set:%1\$s}", context.getString(R.string.temporary))
				)
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}
	}

	fun initialiseLocalStorage(context: Context) {
		val previousSongFilesFolder = mBeatPrompterSongFilesFolder
		val s = context.packageName
		try {
			val m = context.packageManager
			val p = m.getPackageInfo(s, 0)
			mBeatPrompterDataFolder = File(p.applicationInfo.dataDir)
		} catch (e: PackageManager.NameNotFoundException) {
			// There is no way that this can happen.
			Logger.log("Package name not found ", e)
		}

		val songFilesFolder: String
		val useExternalStorage = Preferences.useExternalStorage
		val externalFilesDir = context.getExternalFilesDir(null)
		songFilesFolder = if (useExternalStorage && externalFilesDir != null)
			externalFilesDir.absolutePath
		else
			mBeatPrompterDataFolder!!.absolutePath

		mBeatPrompterSongFilesFolder =
			if (songFilesFolder.isEmpty()) mBeatPrompterDataFolder else File(songFilesFolder)
		if (!mBeatPrompterSongFilesFolder!!.exists())
			if (!mBeatPrompterSongFilesFolder!!.mkdir())
				Logger.log("Failed to create song files folder.")

		if (!mBeatPrompterSongFilesFolder!!.exists())
			mBeatPrompterSongFilesFolder = mBeatPrompterDataFolder

		mTemporarySetListFile = File(mBeatPrompterDataFolder, TEMPORARY_SET_LIST_FILENAME)
		mDefaultMidiAliasesFile = File(mBeatPrompterDataFolder, DEFAULT_MIDI_ALIASES_FILENAME)
		initialiseTemporarySetListFile(false, context)
		try {
			copyAssetsFileToLocalFolder(DEFAULT_MIDI_ALIASES_FILENAME, mDefaultMidiAliasesFile!!)
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}

		mDefaultDownloads.apply {
			clear()
			add(
				SuccessfulDownloadResult(
					FileInfo(
						"idBeatPrompterTemporarySetList",
						"BeatPrompterTemporarySetList",
						Date()
					), mTemporarySetListFile!!
				)
			)
			add(
				SuccessfulDownloadResult(
					FileInfo(
						"idBeatPrompterDefaultMidiAliases",
						context.getString(R.string.default_alias_set_name),
						Date()
					), mDefaultMidiAliasesFile!!
				)
			)
		}

		if (previousSongFilesFolder != null)
			if (previousSongFilesFolder != mBeatPrompterSongFilesFolder)
			// Song file storage folder has changed. We need to clear the cache.
				clearCache(false)
	}

	private fun <TCachedCloudItemType : CachedItem> addToCollection(
		xmlDoc: Document,
		tagName: String,
		parser: (cachedItem: Element) -> TCachedCloudItemType,
		itemSource: PublishSubject<CachedItem>,
		messageSource: PublishSubject<String>
	) {
		val elements = xmlDoc.getElementsByTagName(tagName)
		repeat(elements.length) {
			val element = elements.item(it) as Element
			try {
				val cachedItem = parser(element)
				itemSource.onNext(cachedItem)
				messageSource.onNext(cachedItem.mName)
			} catch (exception: InvalidBeatPrompterFileException) {
				messageSource.onNext(
					exception.message ?: BeatPrompter.getResourceString(R.string.failedToReadDatabaseItem)
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
		messageSource: PublishSubject<String>
	) {
		mCachedCloudItems.clear()
		PARSINGS.forEach {
			addToCollection(
				xmlDoc,
				it.first.findAnnotation<CacheXmlTag>()!!.mTag,
				it.second,
				itemSource,
				messageSource
			)
		}
	}

	fun readDatabase(listener: DatabaseReadListener): Boolean {
		val database = File(mBeatPrompterDataFolder, XML_DATABASE_FILE_NAME)
		val itemSource = PublishSubject.create<CachedItem>()
		val messageSource = PublishSubject.create<String>()
		mCompositeDisposable.add(
			itemSource.subscribe(
				{ listener.onItemRead(it) },
				{ listener.onDatabaseReadError(it) },
				{ listener.onDatabaseReadComplete() })
		)
		mCompositeDisposable.add(messageSource.subscribe {
			Utils.reportProgress(listener, it)
		})

		val result = if (database.exists()) {
			val xmlDoc = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(database)
			readFromXML(xmlDoc, itemSource, messageSource)
			true
		} else
			false
		itemSource.onComplete()
		return result
	}

	private fun writeDatabase() {
		val database = File(mBeatPrompterDataFolder, XML_DATABASE_FILE_NAME)
		if (!database.delete())
			Logger.log("Failed to delete database file.")
		val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		val d = docBuilder.newDocument()
		val root = d.createElement(XML_DATABASE_FILE_ROOT_ELEMENT_TAG)
		d.appendChild(root)
		mCachedCloudItems.writeToXML(d, root)
		val transformer = TransformerFactory.newInstance().newTransformer()
		val output = StreamResult(database)
		val input = DOMSource(d)
		transformer.transform(input, output)
	}

	fun getCacheFolderForStorage(storage: StorageType): CacheFolder {
		val cacheFolderName = Storage.getCacheFolderName(storage)
		return CacheFolder(mBeatPrompterSongFilesFolder!!, cacheFolderName)
	}

	fun clearCache(report: Boolean) {
		// Clear both cache folders
		val cacheFolder = getCacheFolderForStorage(Preferences.storageSystem)
		cacheFolder.clear()
		mCachedCloudItems.clear()
		try {
			writeDatabase()
		} catch (ioe: Exception) {
			Logger.log(ioe)
		}
		EventRouter.sendEventToSongList(Events.CACHE_CLEARED, report)
	}

	fun onCacheUpdated(cache: CachedCloudCollection) {
		mCachedCloudItems = cache
		EventRouter.sendEventToSongList(Events.CACHE_UPDATED, cache)
		try {
			writeDatabase()
		} catch (ioe: Exception) {
			Logger.log(ioe)
		}
	}

	private val cloudPath: String?
		get() {
			return Preferences.cloudPath
		}

	private val includeSubFolders: Boolean
		get() {
			return Preferences.includeSubFolders
		}

	fun canPerformCloudSync(): Boolean {
		return Preferences.storageSystem !== StorageType.Demo && cloudPath != null
	}

	fun performFullCloudSync(parentFragment: Fragment): Boolean {
		return performCloudSync(null, false, parentFragment)
	}

	fun clearTemporarySetList(context: Context) {
		for (slf in mCachedCloudItems.setListFiles)
			if (slf.mFile == mTemporarySetListFile)
				slf.mSetListEntries.clear()
		initialiseTemporarySetListFile(true, context)
		try {
			writeDatabase()
		} catch (ioe: Exception) {
			Logger.log(ioe)
		}
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
		val cs = Storage.getInstance(Preferences.storageSystem, parentFragment)
		val cloudPath = cloudPath
		return if (cloudPath.isNullOrBlank()) {
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
				mSongListEventHandler!!,
				cloudPath,
				includeSubFolders,
				mCachedCloudItems.getFilesToRefresh(fileToUpdate, dependenciesToo)
			).execute(Unit)
			true
		}
	}
}