package com.jiadanni.launcher4d.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.Definitions.ItemPosition
import com.jiadanni.launcher4d.util.Definitions.ItemState

class DatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DATABASE_HOME, null, 1) {

    var db: SQLiteDatabase = writableDatabase

    @Deprecated("Use db property", ReplaceWith("db"))
    val _db: SQLiteDatabase
        get() = db

    @Deprecated("Use context property", ReplaceWith("context"))
    val _context: Context
        get() = context

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // discard the data and start over
        db.execSQL(SQL_DELETE + TABLE_HOME)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun createItem(item: Item, page: Int, itemPosition: ItemPosition) {
        if (com.jiadanni.launcher4d.BuildConfig.DEBUG) {
            Log.i(this::class.java.name, "createItem: ${item.label} (ID: ${item.id})")
        }
        val itemValues = ContentValues().apply {
            put(COLUMN_TIME, item.id)
            put(COLUMN_TYPE, item.type.toString())
            put(COLUMN_LABEL, item.label)
            put(COLUMN_X_POS, item.x)
            put(COLUMN_Y_POS, item.y)

            when (item.type) {
                Item.Type.APP, Item.Type.SHORTCUT -> {
                    Tool.saveIcon(context, Tool.drawableToBitmap(item.icon), item.id.toString())
                    put(COLUMN_DATA, Tool.getIntentAsString(item.intent))
                }
                Item.Type.GROUP -> {
                    val concat = item.items.joinToString(Definitions.DELIMITER) { it.id.toString() }
                    put(COLUMN_DATA, concat + Definitions.DELIMITER)
                }
                Item.Type.ACTION -> {
                    put(COLUMN_DATA, item.actionValue)
                }
                Item.Type.WIDGET -> {
                    val concat = "${item.widgetValue}${Definitions.DELIMITER}${item.spanX}${Definitions.DELIMITER}${item.spanY}"
                    put(COLUMN_DATA, concat)
                }
            }

            put(COLUMN_PAGE, page)
            put(COLUMN_DESKTOP, itemPosition.ordinal)
            // item will always be visible when first added
            put(COLUMN_STATE, 1)
        }

        db.insert(TABLE_HOME, null, itemValues)
    }

    fun saveItem(item: Item) {
        updateItem(item)
    }

    fun saveItem(item: Item, state: ItemState) {
        updateItem(item, state)
    }

    fun saveItem(item: Item, page: Int, itemPosition: ItemPosition) {
        val sqlQuerySpecific = "$SQL_QUERY$TABLE_HOME WHERE $COLUMN_TIME = ?"
        val cursor = db.rawQuery(sqlQuerySpecific, arrayOf(item.id.toString()))
        when (cursor.count) {
            0 -> createItem(item, page, itemPosition)
            1 -> updateItem(item, page, itemPosition)
        }
        cursor.close()
    }

    fun deleteItem(item: Item, deleteSubItems: Boolean) {
        // if the item is a group then remove all entries
        if (deleteSubItems && item.type == Item.Type.GROUP) {
            for (i in item.groupItems) {
                deleteItem(i, deleteSubItems)
            }
        }

        // delete the item itself
        db.delete(TABLE_HOME, "$COLUMN_TIME = ?", arrayOf(item.id.toString()))
    }

    fun deleteItems(app: App) {
        db.delete(
            TABLE_HOME,
            "$COLUMN_TYPE = '${Item.Type.WIDGET}' AND $COLUMN_LABEL LIKE ?",
            arrayOf("${app.packageName}${Definitions.DELIMITER}%")
        )
        db.delete(
            TABLE_HOME,
            "$COLUMN_TYPE = '${Item.Type.APP}' AND $COLUMN_DATA = ?",
            arrayOf(Tool.getIntentAsString(Tool.getIntentFromApp(app)))
        )
    }

    fun getDesktop(): List<List<Item>> {
        val sqlQueryDesktop = SQL_QUERY + TABLE_HOME
        val cursor = db.rawQuery(sqlQueryDesktop, null)
        val desktop = mutableListOf<MutableList<Item>>()

        if (cursor.moveToFirst()) {
            val pageColumnIndex = cursor.getColumnIndex(COLUMN_PAGE)
            val desktopColumnIndex = cursor.getColumnIndex(COLUMN_DESKTOP)
            val stateColumnIndex = cursor.getColumnIndex(COLUMN_STATE)

            do {
                val page = cursor.getString(pageColumnIndex).toInt()
                val desktopVar = cursor.getString(desktopColumnIndex).toInt()
                val stateVar = cursor.getString(stateColumnIndex).toInt()

                while (page >= desktop.size) {
                    desktop.add(mutableListOf())
                }

                if (desktopVar == ItemPosition.Desktop.ordinal && stateVar == ItemState.Visible.ordinal) {
                    desktop[page].add(getSelection(cursor))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return desktop
    }

    fun getDock(): List<Item> {
        val sqlQueryDesktop = SQL_QUERY + TABLE_HOME
        val cursor = db.rawQuery(sqlQueryDesktop, null)
        val dock = mutableListOf<Item>()

        if (cursor.moveToFirst()) {
            val desktopColumnIndex = cursor.getColumnIndex(COLUMN_DESKTOP)
            val stateColumnIndex = cursor.getColumnIndex(COLUMN_STATE)

            do {
                val desktopVar = cursor.getString(desktopColumnIndex).toInt()
                val stateVar = cursor.getString(stateColumnIndex).toInt()

                if (desktopVar == ItemPosition.Dock.ordinal && stateVar == ItemState.Visible.ordinal) {
                    dock.add(getSelection(cursor))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return dock
    }

    fun getItem(id: Int): Item? {
        val sqlQuerySpecific = "$SQL_QUERY$TABLE_HOME WHERE $COLUMN_TIME = ?"
        val cursor = db.rawQuery(sqlQuerySpecific, arrayOf(id.toString()))
        var item: Item? = null

        if (cursor.moveToFirst()) {
            item = getSelection(cursor)
        }
        cursor.close()
        return item
    }

    // update data attribute for an item
    fun updateItem(item: Item) {
        if (com.jiadanni.launcher4d.BuildConfig.DEBUG) {
            Log.i(this::class.java.name, "updateItem: ${item.label} ${item.id}")
        }

        val itemValues = ContentValues().apply {
            put(COLUMN_LABEL, item.label)
            put(COLUMN_X_POS, item.x)
            put(COLUMN_Y_POS, item.y)

            when (item.type) {
                Item.Type.APP, Item.Type.SHORTCUT -> {
                    Tool.saveIcon(context, Tool.drawableToBitmap(item.icon), item.id.toString())
                    put(COLUMN_DATA, Tool.getIntentAsString(item.intent))
                }
                Item.Type.GROUP -> {
                    val concat = item.items.joinToString(Definitions.DELIMITER) { it.id.toString() }
                    put(COLUMN_DATA, concat + Definitions.DELIMITER)
                }
                Item.Type.ACTION -> {
                    put(COLUMN_DATA, item.actionValue)
                }
                Item.Type.WIDGET -> {
                    val concat = "${item.widgetValue}${Definitions.DELIMITER}${item.spanX}${Definitions.DELIMITER}${item.spanY}"
                    put(COLUMN_DATA, concat)
                }
            }
        }

        db.update(TABLE_HOME, itemValues, "$COLUMN_TIME = ?", arrayOf(item.id.toString()))
    }

    // update the state of an item
    fun updateItem(item: Item, state: ItemState) {
        if (com.jiadanni.launcher4d.BuildConfig.DEBUG) {
            Log.i(this::class.java.name, "updateItem: ${item.label} ${item.id}")
        }

        val itemValues = ContentValues().apply {
            put(COLUMN_STATE, state.ordinal)
        }

        db.update(TABLE_HOME, itemValues, "$COLUMN_TIME = ?", arrayOf(item.id.toString()))
    }

    // update the fields only used by the database
    fun updateItem(item: Item, page: Int, itemPosition: ItemPosition) {
        if (com.jiadanni.launcher4d.BuildConfig.DEBUG) {
            Log.i(this::class.java.name, "updateItem: ${item.label} ${item.id}")
        }

        deleteItem(item, false)
        createItem(item, page, itemPosition)
    }

    private fun getSelection(cursor: Cursor): Item {
        val item = Item()
        val id = cursor.getString(cursor.getColumnIndex(COLUMN_TIME)).toInt()
        val type = Item.Type.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_TYPE)))
        val label = cursor.getString(cursor.getColumnIndex(COLUMN_LABEL))
        val x = cursor.getString(cursor.getColumnIndex(COLUMN_X_POS)).toInt()
        val y = cursor.getString(cursor.getColumnIndex(COLUMN_Y_POS)).toInt()
        val data = cursor.getString(cursor.getColumnIndex(COLUMN_DATA))

        item.id = id
        item.label = label
        item.x = x
        item.y = y
        item.type = type

        when (type) {
            Item.Type.APP -> {
                item.intent = Tool.getIntentFromString(data)
                item.shortcutInfo = Tool.getShortcutInfo(context, item.intent.component!!.packageName)
                val app = Setup.get().appLoader.findItemApp(item)
                item.icon = app?.icon
            }
            Item.Type.SHORTCUT -> {
                item.intent = Tool.getIntentFromString(data)
                item.icon = Tool.getIcon(context, item.id.toString())
                if (item.icon == null) {
                    val app = Setup.get().appLoader.findItemApp(item)
                    item.icon = app?.icon
                }
            }
            Item.Type.GROUP -> {
                item.items = mutableListOf()
                val dataSplit = data.split(Definitions.DELIMITER)
                for (string in dataSplit) {
                    if (string.isEmpty()) continue
                    val groupItem = getItem(string.toInt())
                    if (groupItem != null) {
                        item.items.add(groupItem)
                    }
                }
            }
            Item.Type.ACTION -> {
                item.actionValue = data.toInt()
            }
            Item.Type.WIDGET -> {
                val dataSplit = data.split(Definitions.DELIMITER)
                item.widgetValue = dataSplit[0].toInt()
                item.spanX = dataSplit[1].toInt()
                item.spanY = dataSplit[2].toInt()
            }
        }

        return item
    }

    fun addPage(position: Int) {
        db.execSQL(
            "UPDATE $TABLE_HOME SET $COLUMN_PAGE = $COLUMN_PAGE + 1 WHERE $COLUMN_PAGE >= ?",
            arrayOf(position.toString())
        )
    }

    fun removePage(position: Int) {
        db.execSQL(
            "UPDATE $TABLE_HOME SET $COLUMN_PAGE = $COLUMN_PAGE - 1 WHERE $COLUMN_PAGE > ?",
            arrayOf(position.toString())
        )
    }

    fun open() {
        db = writableDatabase
    }

    companion object {
        private const val DATABASE_HOME = "home.db"
        private const val TABLE_HOME = "home"

        private const val COLUMN_TIME = "time"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_X_POS = "x"
        private const val COLUMN_Y_POS = "y"
        private const val COLUMN_DATA = "data"
        private const val COLUMN_PAGE = "page"
        private const val COLUMN_DESKTOP = "desktop"
        private const val COLUMN_STATE = "state"

        private const val SQL_DELETE = "DROP TABLE IF EXISTS "
        private const val SQL_QUERY = "SELECT * FROM "
        private const val SQL_CREATE =
            "CREATE TABLE $TABLE_HOME (" +
                    "$COLUMN_TIME INTEGER PRIMARY KEY," +
                    "$COLUMN_TYPE VARCHAR," +
                    "$COLUMN_LABEL VARCHAR," +
                    "$COLUMN_X_POS INTEGER," +
                    "$COLUMN_Y_POS INTEGER," +
                    "$COLUMN_DATA VARCHAR," +
                    "$COLUMN_PAGE INTEGER," +
                    "$COLUMN_DESKTOP INTEGER," +
                    "$COLUMN_STATE INTEGER)"
    }
}
