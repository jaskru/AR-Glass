/*****************************************************************************/
/*                                                                           */
/*                         ITTIAM MP VIDEO ARM APIS                          */
/*                     ITTIAM SYSTEMS PVT LTD, BANGALORE                     */
/*                             COPYRIGHT(C) 2010                             */
/*                                                                           */
/*  This program  is  proprietary to  Ittiam  Systems  Private  Limited  and */
/*  is protected under Indian  Copyright Law as an unpublished work. Its use */
/*  and  disclosure  is  limited by  the terms  and  conditions of a license */
/*  agreement. It may not be copied or otherwise  reproduced or disclosed to */
/*  persons outside the licensee's organization except in accordance with the*/
/*  terms  and  conditions   of  such  an  agreement.  All  copies  and      */
/*  reproductions shall be the property of Ittiam Systems Private Limited and*/
/*  must bear this notice in its entirety.                                   */
/*                                                                           */
/*****************************************************************************/
/*****************************************************************************/
/*                                                                           */
/*  File Name         : ivd.h                                                */
/*                                                                           */
/*  Description       : This file contains all the necessary structure and   */
/*                      enumeration definitions needed for the Application   */
/*                      Program Interface(API) of the Ittiam Video Decoders  */
/*                                                                           */
/*  List of Functions : None                                                 */
/*                                                                           */
/*  Issues / Problems : None                                                 */
/*                                                                           */
/*  Revision History  :                                                      */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         26 08 2010   100239(RCY)     Draft                                */
/*                                                                           */
/*****************************************************************************/

#ifndef _IVD_H
#define _IVD_H

/*****************************************************************************/
/* Constant Macros                                                           */
/*****************************************************************************/
#define IVD_VIDDEC_MAX_IO_BUFFERS 32
/*****************************************************************************/
/* Typedefs                                                                  */
/*****************************************************************************/

/*****************************************************************************/
/* Enums                                                                     */
/*****************************************************************************/

/* IVD_FRAME_SKIP_MODE_T:Skip mode Enumeration                               */
typedef enum {
    IVD_NO_SKIP                                 = 0xFFFFFFFF,
    IVD_SKIP_P                                  = 0x0,
    IVD_SKIP_B                                  = 0x1,
    IVD_SKIP_I                                  = 0x2,
    IVD_SKIP_IP                                 = 0x3,
    IVD_SKIP_IB                                 = 0x4,
    IVD_SKIP_PB                                 = 0x5,
    IVD_SKIP_IPB                                = 0x6,
    IVD_SKIP_IDR                                = 0x7,
    IVD_SKIP_DEFAULT                            = IVD_NO_SKIP
}IVD_FRAME_SKIP_MODE_T;

/* IVD_VIDEO_DECODE_MODE_T: Set decoder to decode either frame worth of data */
/* or only header worth of data                                              */

typedef enum {

    /* This enables the codec to process all decodable units */
    IVD_DECODE_FRAME                            = 0xFFFFFFFF,

    /* This enables the codec to decode header only */
    IVD_DECODE_HEADER                           = 0x0,
}IVD_VIDEO_DECODE_MODE_T;


/* IVD_DISPLAY_FRAME_OUT_MODE_T: Video Display Frame Output Mode             */

typedef enum {

    /* To set codec to fill output buffers in display order */
    IVD_DISPLAY_FRAME_OUT                       = 0xFFFFFFFF,

    /* To set codec to fill output buffers in decode order */
    IVD_DECODE_FRAME_OUT                        = 0x0,
}IVD_DISPLAY_FRAME_OUT_MODE_T;


/* IVD_API_COMMAND_TYPE_T:API command type                                   */
typedef enum {
	IVD_CMD_VIDEO_NA                          = 0xFFFFFFFF,
    IVD_CMD_VIDEO_CTL                         = IV_CMD_DUMMY_ELEMENT + 1,
    IVD_CMD_VIDEO_DECODE,
    IVD_CMD_GET_DISPLAY_FRAME,
    IVD_CMD_REL_DISP_BUF,
}IVD_API_COMMAND_TYPE_T;

/* IVD_CONTROL_API_COMMAND_TYPE_T: Video Control API command type            */

typedef enum {
	IVD_CMD_NA							= 0xFFFFFFFF,
	IVD_CMD_CTL_GETPARAMS               = 0x0,
	IVD_CMD_CTL_SETPARAMS				= 0x1,
	IVD_CMD_CTL_RESET					= 0x2,
	IVD_CMD_CTL_SETDEFAULT				= 0x3,
	IVD_CMD_CTL_FLUSH					= 0x4,
	IVD_CMD_CTL_GETBUFINFO				= 0x5,
	IVD_CMD_CTL_GETVERSION				= 0x6
}IVD_CONTROL_API_COMMAND_TYPE_T;


/* IVD_ERROR_BITS_T: A UWORD32 container will be used for reporting the error*/
/* code to the application. The first 8 bits starting from LSB have been     */
/* reserved for the codec to report internal error details. The rest of the  */
/* bits will be generic for all video decoders and each bit has an associated*/
/* meaning as mentioned below. The unused bit fields are reserved for future */
/* extenstions and will be zero in the current implementation                */
typedef enum {
    /* Bit 9  - Applied concealment.                                         */
    IVD_APPLIEDCONCEALMENT                      = 0x9,
    /* Bit 10 - Insufficient input data.                                     */
    IVD_INSUFFICIENTDATA                        = 0xa,
    /* Bit 11 - Data problem/corruption.                                     */
    IVD_CORRUPTEDDATA                           = 0xb,
    /* Bit 12 - Header problem/corruption.                                   */
    IVD_CORRUPTEDHEADER                         = 0xc,
    /* Bit 13 - Unsupported feature/parameter in input.                      */
    IVD_UNSUPPORTEDINPUT                        = 0xd,
    /* Bit 14 - Unsupported input parameter orconfiguration.                 */
    IVD_UNSUPPORTEDPARAM                        = 0xe,
    /* Bit 15 - Fatal error (stop the codec).If there is an                  */
    /* error and this bit is not set, the error is a recoverable one.        */
    IVD_FATALERROR                              = 0xf,
    /* Bit 16 - Invalid bitstream. Applies when Bitstream/YUV frame          */
    /* buffer for encode/decode call is made with non-valid or zero u4_size  */
    /* data                                                                  */
    IVD_INVALID_BITSTREAM                       = 0x10,
    /* */
    IVD_INCOMPLETE_BITSTREAM                    = 0x11,
    IVD_ERROR_BITS_T_DUMMY_ELEMENT              = 0xFFFFFFFF
}IVD_ERROR_BITS_T;


/* IVD_CONTROL_API_COMMAND_TYPE_T: Video Control API command type            */
typedef enum {

    IVD_NUM_MEM_REC_FAILED                      = 0x1,
    IVD_NUM_REC_NOT_SUFFICIENT                  = 0x2,
    IVD_REQUESTED_WIDTH_NOT_SUPPPORTED          = 0x3,
    IVD_REQUESTED_HEIGHT_NOT_SUPPPORTED         = 0x4,
    IVD_INIT_DEC_NOT_SUFFICIENT                 = 0x5,
    IVD_INIT_DEC_WIDTH_NOT_SUPPPORTED           = 0x6,
    IVD_INIT_DEC_HEIGHT_NOT_SUPPPORTED          = 0x7,
    IVD_INIT_DEC_MEM_NOT_ALIGNED                = 0x8,
    IND_INIT_DEC_COL_FMT_NOT_SUPPORTED			= 0x9,
    IVD_GET_VERSION_DATABUFFER_SZ_INSUFFICIENT  = 0xa,
    IVD_BUFFER_SIZE_SET_TO_ZERO                 = 0xb,
    IVD_UNEXPECTED_END_OF_STREAM                = 0xc,
    IVD_SEQUENCE_HEADER_NOT_DECODED             = 0xd,
    IVD_STREAM_WIDTH_HEIGHT_NOT_SUPPORTED       = 0xe,
    IVD_MAX_FRAME_LIMIT_REACHED					= 0xf,


    IVD_DUMMY_ELEMENT_FOR_CODEC_EXTENSIONS      = 0x30,
}IVD_ERROR_CODES_T;


/*****************************************************************************/
/* Structure                                                                 */
/*****************************************************************************/
/* structure for passing output buffers to codec during get display buffer   */
/* call                                                                      */
typedef struct {

    /* number of output buffers */
    UWORD32             u4_num_bufs;

    /* list of pointers to output buffers */
    UWORD8              *pu1_bufs[IVD_VIDDEC_MAX_IO_BUFFERS];

    /* sizes of each output buffer */
    UWORD32             u4_min_out_buf_size[IVD_VIDDEC_MAX_IO_BUFFERS];

}ivd_out_bufdesc_t;

/*****************************************************************************/
/*   Initialize decoder                                                      */
/*****************************************************************************/

/* IVD_API_COMMAND_TYPE_T::e_cmd = IVD_CMD_INIT                              */


typedef struct {
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    IVD_API_COMMAND_TYPE_T                  e_cmd;
    UWORD32                                 u4_num_mem_rec;
    UWORD32                                 u4_frm_max_wd;
    UWORD32                                 u4_frm_max_ht;
    IV_COLOR_FORMAT_T                       e_output_format;
    iv_mem_rec_t                            *pv_mem_rec_location;
}ivd_init_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    UWORD32                                 u4_error_code;
}ivd_init_op_t;


/*****************************************************************************/
/*   Video Decode                                                            */
/*****************************************************************************/


/* IVD_API_COMMAND_TYPE_T::e_cmd = IVD_CMD_VIDEO_DECODE                      */


typedef struct {
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    IVD_API_COMMAND_TYPE_T                  e_cmd;
    UWORD32                                 u4_ts;
    UWORD32                                 u4_num_Bytes;
    void                                    *pv_stream_buffer;
}ivd_video_decode_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    UWORD32                                 u4_error_code;
    UWORD32                                 u4_num_bytes_consumed;
    UWORD32                                 u4_pic_wd;
    UWORD32                                 u4_pic_ht;
    IV_PICTURE_CODING_TYPE_T                e_pic_type;
    UWORD32                                 u4_frame_decoded_flag;
    UWORD32                                 u4_new_seq;
}ivd_video_decode_op_t;


/*****************************************************************************/
/*   Get Display Frame                                                       */
/*****************************************************************************/


/* IVD_API_COMMAND_TYPE_T::e_cmd = IVD_CMD_GET_DISPLAY_FRAME                 */

typedef struct
{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;

    IVD_API_COMMAND_TYPE_T                  e_cmd;

    /* output buffer desc */
    ivd_out_bufdesc_t                       s_out_buffer;

}ivd_get_display_frame_ip_t;


typedef struct
{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    UWORD32                                 u4_error_code;
    UWORD32                                 u4_progressive_frame_flag;
    IV_PICTURE_CODING_TYPE_T                e_pic_type;
    UWORD32                                 u4_is_ref_flag;
    IV_COLOR_FORMAT_T                       e_output_format;
    iv_yuv_buf_t                            s_disp_frm_buf;
    IV_FLD_TYPE_T                           u4_fld_type;
    UWORD32                                 u4_ts;
	UWORD32									u4_disp_buf_id;
}ivd_get_display_frame_op_t;


/*****************************************************************************/
/*   Release Display Buffers                                                 */
/*****************************************************************************/

/* IVD_API_COMMAND_TYPE_T::e_cmd = IVD_CMD_REL_DISP_BUFF                     */


typedef struct
{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    IVD_API_COMMAND_TYPE_T                  e_cmd;
    UWORD32                                 u4_ts;
}ivd_rel_disp_buff_ip_t;


typedef struct
{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    UWORD32                                 u4_error_code;
}ivd_rel_disp_buff_op_t;

/*****************************************************************************/
/*   Video control  Flush                                                    */
/*****************************************************************************/
/* IVD_API_COMMAND_TYPE_T::e_cmd            = IVD_CMD_VIDEO_CTL              */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd    = IVD_CMD_ctl_FLUSH          */



typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    IVD_API_COMMAND_TYPE_T                  e_cmd;
    IVD_CONTROL_API_COMMAND_TYPE_T          e_sub_cmd;
}ivd_ctl_flush_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    UWORD32                                 u4_error_code;
}ivd_ctl_flush_op_t;

/*****************************************************************************/
/*   Video control reset                                                     */
/*****************************************************************************/
/* IVD_API_COMMAND_TYPE_T::e_cmd            = IVD_CMD_VIDEO_CTL              */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd    = IVD_CMD_ctl_RESET          */


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    IVD_API_COMMAND_TYPE_T                  e_cmd;
    IVD_CONTROL_API_COMMAND_TYPE_T          e_sub_cmd;
}ivd_ctl_reset_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                 u4_size;
    UWORD32                                 u4_error_code;
}ivd_ctl_reset_op_t;


/*****************************************************************************/
/*   Video control  Set Params                                               */
/*****************************************************************************/
/* IVD_API_COMMAND_TYPE_T::e_cmd        = IVD_CMD_VIDEO_CTL                  */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd=IVD_CMD_ctl_SETPARAMS           */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd=IVD_CMD_ctl_SETDEFAULT          */

typedef struct {
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    IVD_API_COMMAND_TYPE_T                      e_cmd;
    IVD_CONTROL_API_COMMAND_TYPE_T              e_sub_cmd;
    IVD_VIDEO_DECODE_MODE_T                     e_vid_dec_mode;
    UWORD32                                     u4_disp_wd;
    IVD_FRAME_SKIP_MODE_T                       e_frm_skip_mode;
    IVD_DISPLAY_FRAME_OUT_MODE_T                e_frm_out_mode;
}ivd_ctl_set_config_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    UWORD32                                     u4_error_code;
}ivd_ctl_set_config_op_t;

/*****************************************************************************/
/*   Video control:Get Buf Info                                              */
/*****************************************************************************/

/* IVD_API_COMMAND_TYPE_T::e_cmd         = IVD_CMD_VIDEO_CTL                 */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd=IVD_CMD_ctl_GETBUFINFO          */


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    IVD_API_COMMAND_TYPE_T                      e_cmd;
    IVD_CONTROL_API_COMMAND_TYPE_T              e_sub_cmd;
}ivd_ctl_getbufinfo_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    UWORD32                                     u4_error_code;
    /* minimum no of display buffer sets required by codec */
    UWORD32                                     u4_maxNumDisplayBufs;
    /* no of input buffers required for codec */
    UWORD32                                     u4_min_num_in_bufs;
    /* no of output buffers required for codec */
    UWORD32                                     u4_min_num_out_bufs;
    /* sizes of each input buffer required */
    UWORD32                                     u4_min_in_buf_size[IVD_VIDDEC_MAX_IO_BUFFERS];
    /* sizes of each output buffer required */
    UWORD32                                     u4_min_out_buf_size[IVD_VIDDEC_MAX_IO_BUFFERS];
}ivd_ctl_getbufinfo_op_t;


/*****************************************************************************/
/*   Video control:Getstatus Call                                            */
/*****************************************************************************/
/* IVD_API_COMMAND_TYPE_T::e_cmd        = IVD_CMD_VIDEO_CTL                  */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd=IVD_CMD_ctl_GETPARAMS           */


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    IVD_API_COMMAND_TYPE_T                      e_cmd;
    IVD_CONTROL_API_COMMAND_TYPE_T              e_sub_cmd;
}ivd_ctl_getstatus_ip_t;

typedef struct{
    UWORD32                  u4_size;
    UWORD32                  u4_error_code;
    /* minimum no of display buffer sets required by codec */
    UWORD32                  u4_maxNumDisplayBufs;
    UWORD32                  u4_pic_ht;
    UWORD32                  u4_pic_wd;
    UWORD32                  u4_frame_rate;
    UWORD32                  u4_bit_rate;
    IV_CONTENT_TYPE_T        e_content_type;
    IV_COLOR_FORMAT_T        e_output_chroma_format;
    /* no of input buffers required for codec */
    UWORD32                  u4_min_num_in_bufs;
    /* no of output buffers required for codec */
    UWORD32                  u4_min_num_out_bufs;
    /* sizes of each input buffer required */
    UWORD32                  u4_min_in_buf_size[IVD_VIDDEC_MAX_IO_BUFFERS];
    /* sizes of each output buffer required */
    UWORD32                  u4_min_out_buf_size[IVD_VIDDEC_MAX_IO_BUFFERS];
}ivd_ctl_getstatus_op_t;


/*****************************************************************************/
/*   Video control:Get Version Info                                          */
/*****************************************************************************/

/* IVD_API_COMMAND_TYPE_T::e_cmd        = IVD_CMD_VIDEO_CTL                  */
/* IVD_CONTROL_API_COMMAND_TYPE_T::e_sub_cmd=IVD_CMD_ctl_GETVERSION          */


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    IVD_API_COMMAND_TYPE_T                      e_cmd;
    IVD_CONTROL_API_COMMAND_TYPE_T              e_sub_cmd;
    void                                        *pv_version_buffer;
    UWORD32                                     u4_version_buffer_size;
}ivd_ctl_getversioninfo_ip_t;


typedef struct{
    /* u4_size of the structure                                         */
    UWORD32                                     u4_size;
    UWORD32                                     u4_error_code;
}ivd_ctl_getversioninfo_op_t;

#endif /* _IVD_H */

