package com.sepi.market.data.repository

import com.sepi.market.core.ApiResult
import com.sepi.market.core.safeCall
import com.sepi.market.data.remote.RestApi
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.data.remote.dto.ProfileDto
import com.sepi.market.data.remote.dto.UpdateProfileRequest
import com.sepi.market.data.remote.dto.VoucherDto

/**
 * Repository untuk data profil pengguna, manajemen alamat (CRUD Cirebon), dan koleksi voucher.
 */
class ProfileRepository(
    private val restApi: RestApi
) {

    /**
     * Mengambil profil pengguna dari tabel profiles berdasarkan user ID.
     */
    suspend fun getProfile(userId: String): ApiResult<ProfileDto?> {
        return safeCall {
            val list = restApi.getProfile(idFilter = "eq.$userId")
            list.firstOrNull()
        }
    }

    /**
     * Memperbarui nama lengkap dan nomor telepon profil pengguna.
     */
    suspend fun updateProfile(userId: String, fullName: String, phone: String?): ApiResult<List<ProfileDto>> {
        return safeCall {
            restApi.updateProfile(
                idFilter = "eq.$userId",
                request = UpdateProfileRequest(
                    fullName = fullName.trim(),
                    phone = phone?.trim()
                )
            )
        }
    }

    /**
     * Mengambil seluruh alamat pengiriman milik pengguna, diurutkan dari alamat utama.
     */
    suspend fun getAddresses(): ApiResult<List<AddressDto>> {
        return safeCall { restApi.getAddresses() }
    }

    /**
     * Menambahkan alamat pengiriman baru (wajib wilayah Cirebon).
     */
    suspend fun createAddress(address: AddressDto): ApiResult<List<AddressDto>> {
        return safeCall { restApi.createAddress(address) }
    }

    /**
     * Memperbarui alamat pengiriman yang sudah ada.
     */
    suspend fun updateAddress(id: String, address: AddressDto): ApiResult<List<AddressDto>> {
        return safeCall { restApi.updateAddress(idFilter = "eq.$id", address = address) }
    }

    /**
     * Menghapus alamat pengiriman berdasarkan ID.
     */
    suspend fun deleteAddress(id: String): ApiResult<Unit> {
        return safeCall {
            restApi.deleteAddress(idFilter = "eq.$id")
            Unit
        }
    }

    /**
     * Mengambil seluruh daftar voucher yang tersedia dan aktif.
     */
    suspend fun getVouchers(): ApiResult<List<VoucherDto>> {
        return safeCall { restApi.getVouchers() }
    }
}
