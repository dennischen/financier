package com.fsquirrelsoft.financier.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsquirrelsoft.commons.util.CalendarHelper;
import com.fsquirrelsoft.commons.util.Formats;
import com.fsquirrelsoft.commons.util.Logger;
import com.fsquirrelsoft.financier.context.Contexts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_ALL;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_CASHACCOUNT;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_ID;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_INITVAL;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_INITVAL_BD;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_NAME;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_ACC_TYPE;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DETTAG_ALL;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DETTAG_DET_ID;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DETTAG_ID;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DETTAG_TAG_ID;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_ALL;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_ARCHIVED;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_DATE;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_FROM;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_FROM_TYPE;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_ID;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_MONEY;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_MONEY_BD;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_NOTE;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_TO;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_DET_TO_TYPE;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_TAG_ALL;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_TAG_ID;
import static com.fsquirrelsoft.financier.data.DataMeta.COL_TAG_NAME;
import static com.fsquirrelsoft.financier.data.DataMeta.TB_ACC;
import static com.fsquirrelsoft.financier.data.DataMeta.TB_DET;
import static com.fsquirrelsoft.financier.data.DataMeta.TB_DETTAG;
import static com.fsquirrelsoft.financier.data.DataMeta.TB_TAG;

/**
 * 
 * @author dennis
 * 
 */
public class SQLiteDataProvider implements IDataProvider {

    SQLiteDataHelper helper;
    CalendarHelper calHelper;

    public SQLiteDataProvider(SQLiteDataHelper helper, CalendarHelper calHelper) {
        this.helper = helper;
        this.calHelper = calHelper;
    }

    @Override
    public void init() {

    }

    @Override
    public void destroyed() {
        helper.close();
    }

    private String normalizeAccountId(String type, String name) {
        name = name.trim().toLowerCase().replace(' ', '-');
        return type + "-" + name;
    }

    @Override
    public void reset() {
        SQLiteDatabase db = helper.getWritableDatabase();
        helper.onUpgrade(db, -1, db.getVersion());
        detId = 0;
        detId_set = false;
        tagId = 0;
        tagId_set = false;
    }

    @Override
    public Account findAccount(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_ACC, COL_ACC_ALL, COL_ACC_ID + " = ?", new String[] { id }, null, null, null, "1");
        Account acc = null;
        if (c.moveToNext()) {
            acc = new Account();
            applyCursor(acc, c);
        }
        c.close();
        return acc;
    }

    private void applyCursor(Account acc, Cursor c) {
        // to determine if the column for BigDecimal is empty
        boolean isBDEmpty = true;
        int i = 0;
        for (String n : c.getColumnNames()) {
            if (n.equals(COL_ACC_ID)) {
                acc.setId(c.getString(i));
            } else if (n.equals(COL_ACC_NAME)) {
                acc.setName(c.getString(i));
            } else if (n.equals(COL_ACC_TYPE)) {
                acc.setType(c.getString(i));
            } else if (n.equals(COL_ACC_CASHACCOUNT)) {
                // nullable
                acc.setCashAccount(c.getInt(i) == 1);
            } else if (n.equals(COL_ACC_INITVAL)) {
                acc.setInitialValue(c.getDouble(i));
            } else if (n.equals(COL_ACC_INITVAL_BD)) {
                if (!"".equals(c.getString(i))) {
                    acc.setInitialValueBD(new BigDecimal(c.getString(i)));
                    isBDEmpty = false;
                }
            }
            i++;
        }
        if (isBDEmpty) {
            acc.setInitialValueBD(BigDecimal.valueOf(acc.getInitialValue()));
        }
    }

    private void applyContextValue(Account acc, ContentValues values) {
        values.put(COL_ACC_ID, acc.getId());
        values.put(COL_ACC_NAME, acc.getName());
        values.put(COL_ACC_TYPE, acc.getType());
        values.put(COL_ACC_CASHACCOUNT, acc.isCashAccount() ? 1 : 0);
        values.put(COL_ACC_INITVAL, acc.getInitialValue());
        values.put(COL_ACC_INITVAL_BD, Formats.bigDecimalToString(acc.getInitialValueBD()));
    }

    private void applyContextValue(Tag tag, ContentValues values) {
        values.put(COL_ACC_ID, tag.getId());
        values.put(COL_ACC_NAME, tag.getName());
    }

    private void applyContextValue(DetailTag detailTag, ContentValues values) {
        values.put(COL_DETTAG_ID, detailTag.getId());
        values.put(COL_DETTAG_DET_ID, detailTag.getDetailId());
        values.put(COL_DETTAG_TAG_ID, detailTag.getTagId());
    }

    @Override
    public Account findAccount(String type, String name) {
        String id = normalizeAccountId(type, name);
        return findAccount(id);
    }

    @Override
    public List<Account> listAccount(AccountType type) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        if (type == null) {
            c = db.query(TB_ACC, COL_ACC_ALL, null, null, null, null, COL_ACC_ID);
        } else {
            c = db.query(TB_ACC, COL_ACC_ALL, COL_ACC_TYPE + " = ?", new String[] { type.getType() }, null, null, COL_ACC_ID);
        }
        List<Account> result = new ArrayList<Account>();
        Account acc;
        while (c.moveToNext()) {
            acc = new Account();
            applyCursor(acc, c);
            result.add(acc);
        }
        c.close();
        return result;
    }

    @Override
    public void newAccount(Account account) throws DuplicateKeyException {
        String id = normalizeAccountId(account.getType(), account.getName());
        newAccount(id, account);
    }

    public String toAccountId(Account account) {
        String id = normalizeAccountId(account.getType(), account.getName());
        return id;
    }

    public synchronized void newAccount(String id, Account account) throws DuplicateKeyException {
        if (findAccount(id) != null) {
            throw new DuplicateKeyException("duplicate account id " + id);
        }
        newAccountNoCheck(id, account);
    }

    @Override
    public void newAccountNoCheck(String id, Account account) {
        if (Contexts.DEBUG) {
            Logger.d("new account " + id);
        }
        account.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(account, cv);
        db.insertOrThrow(TB_ACC, null, cv);
    }

    @Override
    public boolean updateAccount(String id, Account account) {
        Account acc = findAccount(id);
        if (acc == null) {
            return false;
        }

        // reset id, id is following the name;
        String newid = normalizeAccountId(account.getType(), account.getName());
        account.setId(newid);

        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(account, cv);

        // use old id to update
        int r = db.update(TB_ACC, cv, COL_ACC_ID + " = ?", new String[] { id });

        if (r > 0) {
            // update the refereted detail id
            cv = new ContentValues();
            cv.put(COL_DET_FROM, newid);
            cv.put(COL_DET_FROM_TYPE, account.getType());
            db.update(TB_DET, cv, COL_DET_FROM + " = ?", new String[] { id });

            cv = new ContentValues();
            cv.put(COL_DET_TO, newid);
            cv.put(COL_DET_TO_TYPE, account.getType());
            db.update(TB_DET, cv, COL_DET_TO + " = ?", new String[] { id });
        }

        return r > 0;
    }

    @Override
    public boolean deleteAccount(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int r = db.delete(TB_ACC, COL_ACC_ID + " = ?", new String[] { id });
        return r > 0;
    }

    /**
     * detail impl.
     */

    private void applyCursor(Detail det, Cursor c) {
        // to determine if the column for BigDecimal is empty
        boolean isBDEmpty = true;
        int i = 0;
        for (String n : c.getColumnNames()) {
            if (n.equals(COL_DET_ID)) {
                det.setId(c.getInt(i));
            } else if (n.equals(COL_DET_FROM)) {
                det.setFrom(c.getString(i));
            } else if (n.equals(COL_DET_TO)) {
                det.setTo(c.getString(i));
            } else if (n.equals(COL_DET_DATE)) {
                det.setDate(new Date(c.getLong(i)));
            } else if (n.equals(COL_DET_MONEY)) {
                det.setMoney(c.getDouble(i));
            } else if (n.equals(COL_DET_MONEY_BD)) {
                if (!"".equals(c.getString(i))) {
                    det.setMoneyBD(new BigDecimal(c.getString(i)));
                    isBDEmpty = false;
                }
            } else if (n.equals(COL_DET_ARCHIVED)) {
                det.setArchived((c.getInt(i) == 1));
            } else if (n.equals(COL_DET_NOTE)) {
                det.setNote(c.getString(i));
            }
            i++;
        }
        if (isBDEmpty) {
            det.setMoneyBD(BigDecimal.valueOf(det.getMoney()));
        }
    }

    private void applyContextValue(Detail det, ContentValues values) {
        values.put(COL_DET_ID, det.getId());
        values.put(COL_DET_FROM, det.getFrom());
        values.put(COL_DET_FROM_TYPE, det.getFromType());
        values.put(COL_DET_TO, det.getTo());
        values.put(COL_DET_TO_TYPE, det.getToType());
        values.put(COL_DET_DATE, calHelper.toDayMiddle(det.getDate()).getTime());
        values.put(COL_DET_MONEY, det.getMoney());
        values.put(COL_DET_MONEY_BD, Formats.bigDecimalToString(det.getMoneyBD()));
        values.put(COL_DET_ARCHIVED, det.isArchived() ? 1 : 0);
        values.put(COL_DET_NOTE, det.getNote());
    }

    @Override
    public Detail findDetail(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_DET, COL_DET_ALL, COL_DET_ID + " = " + id, null, null, null, null, "1");
        Detail det = null;
        if (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
        }
        c.close();
        return det;
    }

    static int detId = 0;
    static boolean detId_set;

    public synchronized int nextDetailId() {
        if (!detId_set) {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT MAX(" + DataMeta.COL_DET_ID + ") FROM " + DataMeta.TB_DET, null);
            if (c.moveToNext()) {
                detId = c.getInt(0);
            }
            // detId_set = true;
            c.close();
        }
        return ++detId;
    }

    @Override
    public void newDetail(Detail detail) {
        int id = nextDetailId();
        try {
            newDetail(id, detail);
        } catch (DuplicateKeyException e) {
            Logger.e(e.getMessage(), e);
        }
    }

    public void newDetail(int id, Detail detail) throws DuplicateKeyException {
        if (findDetail(id) != null) {
            throw new DuplicateKeyException("duplicate detail id " + id);
        }
        newDetailNoCheck(id, detail);
    }

    @Override
    public void newDetailNoCheck(int id, Detail detail) {
        if (Contexts.DEBUG) {
            Logger.d("new detail " + id + "," + detail.getNote());
        }
        first = null;
        detail.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(detail, cv);
        db.insertOrThrow(TB_DET, null, cv);
    }

    @Override
    public boolean updateDetail(int id, Detail detail) {
        Detail det = findDetail(id);
        if (det == null) {
            return false;
        }
        first = null;
        // set id, detail might have a dirty id from copy or zero
        detail.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(detail, cv);

        // use old id to update
        int r = db.update(TB_DET, cv, COL_DET_ID + " = " + id, null);
        return r > 0;
    }

    @Override
    public boolean deleteDetail(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        first = null;
        int r = db.delete(TB_DET, COL_DET_ID + " = " + id, null);
        db.delete(TB_DETTAG, COL_DETTAG_DET_ID + " = " + id, null);
        return r > 0;
    }

    @Override
    public List<Detail> listAllDetail() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, null, null, null, null, DET_ORDERBY);
        List<Detail> result = new ArrayList<Detail>();
        Detail det;
        while (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    static final String DET_ORDERBY = COL_DET_DATE + " DESC," + COL_DET_ID + " DESC";

    @Override
    public List<Detail> listDetail(Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Detail> result = new ArrayList<Detail>();
        Detail det;
        while (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Detail> listDetail(Date start, Date end, String note, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }
        if (note != null && !"".equals(note.trim())) {
            where.append(" AND ");
            where.append(COL_DET_NOTE + " like '%" + note + "%'");
        }

        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Detail> result = new ArrayList<Detail>();
        Detail det;
        while (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Detail> listDetail(Account account, int mode, Date start, Date end, int max) {
        return listDetail(account.getId(), mode, start, end, max);
    }

    @Override
    public List<Detail> listDetail(String accountId, int mode, Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<String>();
        String nestedId = accountId + ".%";
        where.append(" 1=1 ");
        if (mode == LIST_DETAIL_MODE_FROM) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_DETAIL_MODE_TO) {
            where.append(" AND (");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_DETAIL_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? OR ");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
            args.add(accountId);
            args.add(nestedId);
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }
        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }
        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), wherearg, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Detail> result = new ArrayList<Detail>();
        Detail det;
        while (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Detail> listDetail(AccountType type, int mode, Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (mode == LIST_DETAIL_MODE_FROM) {
            where.append(" AND ");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_DETAIL_MODE_TO) {
            where.append(" AND ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_DETAIL_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "' OR ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "')");
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Detail> result = new ArrayList<Detail>();
        Detail det;
        while (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Detail> listDetail(Tag tag, int mode, Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder where = new StringBuilder();
        where.append("(dt.tagid_= ").append(tag.getId()).append(" or t.nm_ like '").append(tag.getName()).append(".%')");

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        Cursor c = null;
        c = db.rawQuery("select d.* from fsf_dettag dt inner join fsf_det d on d.id_ = dt.detid_ inner join fsf_tag t on t.id_ = dt.tagid_ where " + where.toString() + " order by d.dt_ desc, d.id_",
                null);
        List<Detail> result = new ArrayList<Detail>();
        Detail det;
        while (c.moveToNext()) {
            det = new Detail();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public int countDetail(Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT COUNT(").append(COL_DET_ID).append(") FROM ").append(TB_DET);

        if (where.length() > 0) {
            query.append(" WHERE ").append(where);
        }

        Cursor c = db.rawQuery(query.toString(), null);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public int countDetail(Account account, int mode, Date start, Date end) {
        return countDetail(account.getId(), mode, start, end);
    }

    @Override
    public int countDetail(String accountId, int mode, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String nestedId = accountId + ".%";
        StringBuilder query = new StringBuilder();
        List<String> args = new ArrayList<String>();
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (mode == LIST_DETAIL_MODE_FROM) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_DETAIL_MODE_TO) {
            where.append(" AND (");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_DETAIL_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? OR ");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
            args.add(accountId);
            args.add(nestedId);
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT COUNT(").append(COL_DET_ID).append(") FROM ").append(TB_DET);

        if (where.length() > 0) {
            query.append(" WHERE ").append(where);
        }

        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }

        Cursor c = db.rawQuery(query.toString(), wherearg);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public int countDetail(AccountType type, int mode, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");

        if (mode == LIST_DETAIL_MODE_FROM) {
            where.append(" AND ");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_DETAIL_MODE_TO) {
            where.append(" AND ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_DETAIL_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "' OR ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "')");
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT COUNT(").append(COL_DET_ID).append(") FROM ").append(TB_DET);

        if (where.length() > 0) {
            query.append(" WHERE ").append(where);
        }

        Cursor c = db.rawQuery(query.toString(), null);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public int countDetail(Tag tag, int mode, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder where = new StringBuilder();
        where.append("(dt.tagid_= ").append(tag.getId()).append(" or t.nm_ like '").append(tag.getName()).append(".%')");

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        Cursor c = db.rawQuery("select count(d.id_) from fsf_dettag dt inner join fsf_det d on d.id_ = dt.detid_ inner join fsf_tag t on t.id_ = dt.tagid_ where " + where.toString(), null);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public BigDecimal sumFrom(AccountType type, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_FROM_TYPE).append(" = '").append(type.type).append("'");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT ").append(COL_DET_MONEY_BD).append(" FROM ").append(TB_DET).append(where);

        Cursor c = db.rawQuery(query.toString(), null);

        BigDecimal r = BigDecimal.ZERO;
        while (c.moveToNext()) {
            r = r.add(new BigDecimal(c.getString(0) == null ? "0" : c.getString(0)));
        }

        c.close();
        return r;
    }

    @Override
    public BigDecimal sumFrom(Account acc, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();
        List<String> args = new ArrayList<String>();
        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_FROM).append(" = ? ");
        args.add(acc.getId());
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT ").append(COL_DET_MONEY_BD).append(" FROM ").append(TB_DET).append(where);

        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }

        Cursor c = db.rawQuery(query.toString(), wherearg);

        BigDecimal r = BigDecimal.ZERO;
        while (c.moveToNext()) {
            r = r.add(new BigDecimal(c.getString(0) == null ? "0" : c.getString(0)));
        }

        c.close();
        return r;
    }

    @Override
    public BigDecimal sumTo(AccountType type, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_TO_TYPE).append(" = '").append(type.type).append("'");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT ").append(COL_DET_MONEY_BD).append(" FROM ").append(TB_DET).append(where);

        Cursor c = db.rawQuery(query.toString(), null);

        BigDecimal r = BigDecimal.ZERO;
        while (c.moveToNext()) {
            r = r.add(new BigDecimal(c.getString(0) == null ? "0" : c.getString(0)));
        }

        c.close();
        return r;
    }

    @Override
    public BigDecimal sumTo(Account acc, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();
        List<String> args = new ArrayList<String>();
        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_TO).append(" = ?");
        args.add(acc.getId());
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT ").append(COL_DET_MONEY_BD).append(" FROM ").append(TB_DET).append(where);
        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }

        Cursor c = db.rawQuery(query.toString(), wherearg);

        BigDecimal r = BigDecimal.ZERO;
        while (c.moveToNext()) {
            r = r.add(new BigDecimal(c.getString(0) == null ? "0" : c.getString(0)));
        }

        c.close();
        return r;
    }

    @Override
    public void deleteAllAccount() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_ACC, null, null);
        return;

    }

    @Override
    public void deleteAllDetail() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_DET, null, null);
        detId = 0;
        detId_set = false;
        first = null;
        return;

    }

    @Override
    public void deleteAllTag() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_TAG, null, null);
        tagId = 0;
        tagId_set = false;
        return;
    }

    @Override
    public void deleteAllDetailTag() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_DETTAG, null, null);
        detailTagId = 0;
        detailTagId_set = false;
        return;

    }

    @Override
    public boolean deleteTag(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int r = db.delete(TB_TAG, COL_TAG_ID + " = " + id, null);
        return r > 0;
    }

    Detail first = null;

    @Override
    public Detail getFirstDetail() {
        if (first != null)
            return first;
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, COL_DET_DATE, Integer.toString(1));
        first = null;
        if (c.moveToNext()) {
            first = new Detail();
            applyCursor(first, c);
        }
        c.close();
        return first;
    }

    @Override
    public BigDecimal sumInitialValue(AccountType type) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_ACC_TYPE).append(" = '").append(type.type).append("'");

        query.append("SELECT ").append(COL_ACC_INITVAL_BD).append(" FROM ").append(TB_ACC).append(where);

        Cursor c = db.rawQuery(query.toString(), null);

        BigDecimal r = BigDecimal.ZERO;
        while (c.moveToNext()) {
            r = r.add(new BigDecimal(c.getString(0) == null ? "0" : c.getString(0)));
        }

        c.close();
        return r;
    }

    @Override
    public List<Tag> listAllTags() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_TAG, COL_TAG_ALL, null, null, null, null, COL_TAG_NAME);
        List<Tag> result = new ArrayList<Tag>();
        Tag tag;
        while (c.moveToNext()) {
            tag = new Tag();
            applyCursor(tag, c);
            result.add(tag);
        }
        c.close();
        return result;
    }

    @Override
    public List<DetailTag> listSelectedDetailTags(int detailId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_DETTAG, COL_DETTAG_ALL, COL_DETTAG_DET_ID + " = ?", new String[] { String.valueOf(detailId) }, null, null, COL_TAG_ID);
        List<DetailTag> result = new ArrayList<DetailTag>();
        DetailTag detailTag;
        while (c.moveToNext()) {
            detailTag = new DetailTag();
            applyCursor(detailTag, c);
            result.add(detailTag);
        }
        c.close();
        return result;
    }

    private void applyCursor(Tag tag, Cursor c) {
        int i = 0;
        for (String n : c.getColumnNames()) {
            if (n.equals(COL_TAG_ID)) {
                tag.setId(c.getInt(i));
            } else if (n.equals(COL_TAG_NAME)) {
                tag.setName(c.getString(i));
            }
            i++;
        }
    }

    private void applyCursor(DetailTag detailTag, Cursor c) {
        int i = 0;
        for (String n : c.getColumnNames()) {
            if (n.equals(COL_DETTAG_ID)) {
                detailTag.setId(c.getInt(i));
            } else if (n.equals(COL_DETTAG_DET_ID)) {
                detailTag.setDetailId(c.getInt(i));
            } else if (n.equals(COL_DETTAG_TAG_ID)) {
                detailTag.setTagId(c.getInt(i));
            }
            i++;
        }
    }

    static int tagId = 0;
    static boolean tagId_set;

    public synchronized int nextTagId() {
        if (!tagId_set) {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT MAX(" + DataMeta.COL_TAG_ID + ") FROM " + DataMeta.TB_TAG, null);
            if (c.moveToNext()) {
                tagId = c.getInt(0);
            }
            // tagId_set = true;
            c.close();
        }
        return ++tagId;
    }

    @Override
    public void newTag(Tag tag) throws DuplicateKeyException {
        int id = nextTagId();
        newTag(id, tag);
    }

    @Override
    public void newTag(int id, Tag tag) throws DuplicateKeyException {
        if (findTag(id) != null) {
            throw new DuplicateKeyException("duplicate tag id " + id);
        }
        newTagNoCheck(id, tag);
    }

    @Override
    public void newTagNoCheck(int id, Tag tag) {
        if (Contexts.DEBUG) {
            Logger.d("new tag " + id + "," + tag.getName());
        }
        tag.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(tag, cv);
        db.insertOrThrow(TB_TAG, null, cv);
    }

    @Override
    public boolean updateTag(int id, Tag tag) {
        Tag t = findTag(id);
        if (t == null) {
            return false;
        }
        tag.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(tag, cv);
        int r = db.update(TB_TAG, cv, COL_TAG_ID + " = " + id, null);
        return r > 0;
    }

    @Override
    public Tag findTag(String name) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_TAG, COL_TAG_ALL, COL_TAG_NAME + " = ?", new String[] { name }, null, null, null, "1");
        Tag tag = null;
        if (c.moveToNext()) {
            tag = new Tag();
            applyCursor(tag, c);
        }
        c.close();
        return tag;
    }

    @Override
    public Tag findTag(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_TAG, COL_TAG_ALL, COL_TAG_ID + " = " + id, null, null, null, null, "1");
        Tag tag = null;
        if (c.moveToNext()) {
            tag = new Tag();
            applyCursor(tag, c);
        }
        c.close();
        return tag;
    }

    @Override
    public DetailTag findDetailTag(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_DETTAG, COL_DETTAG_ALL, COL_DETTAG_ID + " = " + id, null, null, null, null, "1");
        DetailTag detail = null;
        if (c.moveToNext()) {
            detail = new DetailTag();
            applyCursor(detail, c);
        }
        c.close();
        return detail;
    }

    @Override
    public void deleteTagsByDetailId(int detailId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_DETTAG, COL_DETTAG_DET_ID + " = " + detailId, null);
        detailTagId = 0;
        detailTagId_set = false;
        return;
    }

    static int detailTagId = 0;
    static boolean detailTagId_set;

    public synchronized int nextDetailTagId() {
        if (!detailTagId_set) {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT MAX(" + DataMeta.COL_DETTAG_ID + ") FROM " + DataMeta.TB_DETTAG, null);
            if (c.moveToNext()) {
                detailTagId = c.getInt(0);
            }
            // detailTagId_set = true;
            c.close();
        }
        return ++detailTagId;
    }

    @Override
    public void newDetailTag(DetailTag detailTag) {
        int id = nextDetailTagId();
        try {
            newDetailTag(id, detailTag);
        } catch (DuplicateKeyException e) {
            Logger.e(e.getMessage(), e);
        }
    }

    @Override
    public void newDetailTag(int id, DetailTag detailTag) throws DuplicateKeyException {
        if (findDetailTag(id) != null) {
            throw new DuplicateKeyException("duplicate detailTag id " + id);
        }
        newDetailTagNoCheck(id, detailTag);
    }

    @Override
    public void newDetailTagNoCheck(int id, DetailTag detailTag) {
        if (Contexts.DEBUG) {
            Logger.d("new detailTag " + id + "," + detailTag.getTagId());
        }
        detailTag.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(detailTag, cv);
        db.insertOrThrow(TB_DETTAG, null, cv);
    }

    @Override
    public List<Map<String, Object>> listDetailTagData(Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuffer where = new StringBuffer(" where");
        where.append(" 1=1 ");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }
        Cursor c = null;
        c = db.rawQuery(
                "select t.nm_, d.mnb_, d.tot_, dt.tagid_ from fsf_dettag dt inner join fsf_det d on d.id_ = dt.detid_ inner join fsf_tag t on t.id_ = dt.tagid_" + where.toString() + " order by t.nm_",
                null);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> rowData;
        while (c.moveToNext()) {
            rowData = new HashMap<String, Object>();
            rowData.put("name", c.getString(0));
            rowData.put("money", c.getString(1));
            rowData.put("type", c.getString(2));
            rowData.put("tagId", c.getString(3));
            result.add(rowData);
        }
        c.close();
        return result;
    }

    @Override
    public List<DetailTag> listAllDetailTags() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_DETTAG, COL_DETTAG_ALL, null, null, null, null, COL_DETTAG_ID);
        List<DetailTag> result = new ArrayList<DetailTag>();
        DetailTag dettag;
        while (c.moveToNext()) {
            dettag = new DetailTag();
            applyCursor(dettag, c);
            result.add(dettag);
        }
        c.close();
        return result;
    }

}
