package com.ettrema.db;

import com.ettrema.db.Table.Field;
import com.ettrema.db.Table.Index;
import com.ettrema.db.types.FieldType;
import com.ettrema.db.types.FieldTypes;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 *
 * @author bradm
 */
public class TableXmlHelper {

	private final Namespace ns;

	public TableXmlHelper(Namespace ns) {
		this.ns = ns;
	}

	public Element toXml(Table table) {
		Element el = createElement("table");
		el.setAttribute("tableName", table.tableName);
		if (table.getPk() != null) {
			el.setAttribute("pk", table.getPk().getName());
		}
		toXmlFields(el, table.getFields());
		toXmlIndexes(el, table.getIndexes());
		return el;
	}

	public Table fromXml(Element el) {
		String s = el.getAttributeValue("tableName");
		Table table = new Table(s);
		System.out.println("tablename: " + s);
		fromXmlFields(table, el);
		System.out.println("fields: " + table.getFields().size());
		fromXmlIndexes(table, el);
		String sPk = getAtt(el, "pk");
		if (sPk != null && sPk.length() > 0) {
			table.setPrimaryKey(table.getField(sPk));
		}
		return table;
	}

	private void toXmlFields(Element el, List<Field> fields) {
		Element elFields = createElement("fields");
		el.addContent(elFields);
		for (Field f : fields) {
			appendField(elFields, f);
		}
	}

	private void fromXmlFields(Table table, Element el) {
		Element elFields = el.getChild("fields", ns);
		if (elFields == null) {
			return;
		}

		List<Element> list = toElements(elFields.getChildren());
		for (Element elField : list) {
			addFieldToTable(table, elField);
		}

	}

	private void appendField(Element elFields, Field f) {
		Element el = createElement("field");
		elFields.addContent(el);
		setAtt(el, "name", f.getName());
		setAtt(el, "nullable", f.nullable);
		setAtt(el, "type", f.getType().toString());
	}

	private void addFieldToTable(Table table, Element el) {
		String name = el.getAttributeValue("name");
		boolean nullable = getAttBool(el, "nullable");
		String sType = getAtt(el, "type");
		FieldType type = FieldTypes.fromName(sType);
		table.add(name, type, nullable);
	}

	private Element createElement(String name) {
		return new Element(name, ns);
	}

	private void setAtt(Element el, String name, String value) {
		if (value != null) {
			el.setAttribute(name, value);
		} else {
			el.removeAttribute(name);
		}
	}

	private String getAtt(Element el, String name) {
		return el.getAttributeValue(name);
	}

	private boolean getAttBool(Element el, String name) {
		String s = getAtt(el, name);
		if (s == null || s.length() == 0) {
			return false;
		} else {
			return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
		}
	}

	private void setAtt(Element el, String name, boolean value) {
		el.setAttribute(name, value + "");
	}

	private void toXmlIndexes(Element el, List<Index> indexes) {
		Element elIndexes = createElement("indexes");
		el.addContent(elIndexes);
		for (Index f : indexes) {
			appendIndex(elIndexes, f);
		}
	}

	private void appendIndex(Element elIndexes, Index index) {
		Element el = createElement("index");
		elIndexes.addContent(el);
		setAtt(el, "name", index.getName());
		for (Field f : index) {
			Element elFieldRef = createElement("fieldref");
			el.addContent(elFieldRef);
			setAtt(elFieldRef, "name", f.name);
		}
	}

	private void addIndexToTable(Table table, Element elIndex) {
		String name = getAtt(elIndex, "name");
		List<Element> list = toElements(elIndex.getChildren());
		List<Field> fields = new ArrayList<Field>();
		for (Element elFieldRef : list) {
			String fieldName = getAtt(elFieldRef, "name");
			Field f = table.getField(fieldName);
			if (f == null) {
				throw new RuntimeException("Field not found: " + fieldName);
			}
			fields.add(f);
		}
		table.addIndex(name, fields); 
	}

	private List<Element> toElements(List children) {
		List<Element> list = new ArrayList<Element>();
		list.addAll(children);
		return list;
	}

	private void fromXmlIndexes(Table table, Element el) {
		Element elIndexes = el.getChild("indexes", ns);
		if (elIndexes == null) {
			return;
		}

		List<Element> list = toElements(elIndexes.getChildren());
		for (Element elIndex : list) {
			addIndexToTable(table, elIndex);
		}
	}

	public Namespace getNs() {
		return ns;
	}
	

}
