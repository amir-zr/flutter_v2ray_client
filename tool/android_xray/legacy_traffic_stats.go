package libv2ray

// QueryStats reads and resets one outbound traffic counter for older clients.
func (x *CoreController) QueryStats(tag, direction string) int64 {
	x.coreMutex.Lock()
	defer x.coreMutex.Unlock()

	if x.statsManager == nil {
		return 0
	}
	counter := x.statsManager.GetCounter("outbound>>>" + tag + ">>>traffic>>>" + direction)
	if counter == nil {
		return 0
	}
	return counter.Set(0)
}
