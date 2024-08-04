package com.stevenfrew.beatprompter.comm

data class ConnectionDescriptor(val name: String, val commType: CommunicationType) {
	override fun equals(other: Any?): Boolean =
		if (other is ConnectionDescriptor)
			name == other.name && commType == other.commType
		else
			false

	override fun hashCode(): Int {
		var result = name.hashCode()
		result = 31 * result + commType.hashCode()
		return result
	}

	override fun toString(): String = "$commType device '$name'"
}
