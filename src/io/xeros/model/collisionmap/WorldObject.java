package io.xeros.model.collisionmap;

import java.util.Objects;

import io.xeros.Server;
import io.xeros.model.entity.player.Position;
import io.xeros.model.world.objects.GlobalObject;
import io.xeros.model.world.objects.GlobalObjects;

public class WorldObject {

	public final int x, y, height, id, type, face;

	public WorldObject(int id, int x, int y, int height, int face) {
		this(id, x, y, height, 10, face);
	}

	public WorldObject(int id, int x, int y, int height, int type, int face) {
		this.x = x;
		this.y = y;
		this.height = height;
		this.id = id;
		this.type = type;
		this.face = face;
	}

	public GlobalObject toGlobalObject() {
		return new GlobalObject(id, x, y, height, face, type);
	}

	public Position getPosition() {
		return new Position(x, y, height);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WorldObject that = (WorldObject) o;
		return x == that.x &&
				y == that.y &&
				height == that.height &&
				id == that.id &&
				type == that.type &&
				face == that.face;
	}

	public ObjectDef getObjectDef() {
		return ObjectDef.getObjectDef(id);
	}

	public Position getObjectSize() {
		ObjectDef def = ObjectDef.getObjectDef(id);
		if (def == null) {
			return new Position(0, 0);
		}

		int xLength;
		int yLength;
		if (face != 1 && face != 3) {
			xLength = def.xLength();
			yLength = def.yLength();
		} else {
			xLength = def.yLength();
			yLength = def.xLength();
		}
		return new Position(xLength, yLength);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, height, id, type, face);
	}

	@Override
	public String toString() {
		return "WorldObject{" +
				"x=" + x +
				", y=" + y +
				", height=" + height +
				", id=" + id +
				", type=" + type +
				", face=" + face +
				'}';
	}

	public int getId() {
		return id;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getType() {
		return type;
	}

	public int getHeight() {
		return height;
	}

	public int getFace() {
		return face;
	}

    public void replace(int objectId) {
		Server.getGlobalObjects().add(new GlobalObject(objectId, x, y, height, face, type, 200, id));
    }
}