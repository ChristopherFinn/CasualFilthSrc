package io.xeros.model.collisionmap;

public class RegionData {
	private final int regionHash;
    private final int landscape;
    private final int objects;

	public int getRegionHash() {
		return regionHash;
	}

	public int getLandscape() {
		return landscape;
	}

	public int getObjects() {
		return objects;
	}

	public RegionData(int regionHash, int landscape, int objects) {
		this.regionHash = regionHash;
		this.landscape = landscape;
		this.objects = objects;
	}
	
}