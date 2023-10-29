package com.gallen.pojos.sensitive;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import com.gallen.common.annotation.IdEncrypt;
import lombok.Data;

/**
 * 敏感词表
 * @TableName tb_sensitive_word
 */
@TableName(value ="tb_sensitive_word")
@Data
public class SensitiveWord implements Serializable {
    /**
     * 
     */
    @TableId(value = "id")
    @IdEncrypt
    private Long id;

    /**
     * 敏感词
     */
    @TableField(value = "word")
    private String word;

    /**
     * 
     */
    @TableField(value = "create_timeda")
    private Date createTimeda;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        SensitiveWord other = (SensitiveWord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getWord() == null ? other.getWord() == null : this.getWord().equals(other.getWord()))
            && (this.getCreateTimeda() == null ? other.getCreateTimeda() == null : this.getCreateTimeda().equals(other.getCreateTimeda()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getWord() == null) ? 0 : getWord().hashCode());
        result = prime * result + ((getCreateTimeda() == null) ? 0 : getCreateTimeda().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", word=").append(word);
        sb.append(", createTimeda=").append(createTimeda);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}