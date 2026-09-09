package com.sihoo.ai_agent_virtual_mobile.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sihoo.ai_agent_virtual_mobile.character.OutfitType
import com.sihoo.ai_agent_virtual_mobile.character.PetRepository

class MainViewModel(
    private val repository: PetRepository
) : ViewModel() {
    private val _outfit = MutableLiveData(repository.getOutfit())
    val outfit: LiveData<OutfitType> = _outfit

    fun selectOutfit(outfitType: OutfitType) {
        if (outfitType == _outfit.value) {
            return
        }
        repository.saveOutfit(outfitType)
        _outfit.value = outfitType
    }

    class Factory(
        private val repository: PetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
    }
}
